import json
import os
from pathlib import Path
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

try:
    from neo4j import GraphDatabase
    _driver = GraphDatabase.driver(
        os.getenv("NEO4J_URI", "bolt://localhost:7687"),
        auth=(os.getenv("NEO4J_USER", "neo4j"),
              os.getenv("NEO4J_PASSWORD", "giftgpt123"))
    )
    _driver.verify_connectivity()
    _NEO4J_OK = True
except Exception:
    _NEO4J_OK = False
    _driver = None


class KGQueryRequest(BaseModel):
    recipient_id: Optional[int] = None
    cypher: Optional[str] = None
    personality_tag: Optional[str] = None
    occasion: Optional[str] = None
    budget_min: Optional[float] = None
    budget_max: Optional[float] = None


class KGProduct(BaseModel):
    id: int
    name: str
    price: float
    platform: Optional[str] = None
    image_url: Optional[str] = None
    matched_tags: list[str] = []
    reasoning_chain: str = ""


class KGQueryResponse(BaseModel):
    products: list[dict]
    related_tags: list[str]
    reasoning_chain: str
    neo4j_connected: bool = False


_OCCASION_NAMES = {
    "birthday": "\u751f\u65e5", "anniversary": "\u7eaa\u5ff5\u65e5",
    "valentines": "\u60c5\u4eba\u8282", "festival": "\u8282\u5e86",
    "graduation": "\u6bd5\u4e1a", "proposal": "\u6c42\u5a5a",
    "mothers_day": "\u6bcd\u4eb2\u8282", "fathers_day": "\u7236\u4eb2\u8282",
    "teachers_day": "\u6559\u5e08\u8282", "christmas": "\u5723\u8bde",
    "thank_you": "\u611f\u8c22", "other": "\u5176\u4ed6",
}


@router.post("/kg/query", response_model=KGQueryResponse)
async def query_knowledge_graph(request: KGQueryRequest):
    """Knowledge graph multi-hop reasoning query (Neo4j)."""
    if not _NEO4J_OK:
        return KGQueryResponse(
            products=[],
            related_tags=[],
            reasoning_chain="Neo4j not connected. Run: docker compose up -d neo4j",
            neo4j_connected=False,
        )

    if request.cypher:
        # Direct Cypher execution (for debugging / demo)
        with _driver.session() as session:
            result = session.run(request.cypher)
            records = [r.data() for r in result]
        return KGQueryResponse(
            products=records,
            related_tags=[],
            reasoning_chain="Custom Cypher query",
            neo4j_connected=True,
        )

    # Multi-hop: Recipient -> HAS_TAG -> Tag -> PREFERS_CATEGORY -> Category
    #          <- BELONGS_TO <- Product -> BELONGS_TO -> Category -> FIT_OCCASION -> Occasion
    occasion = request.occasion or "\u751f\u65e5"
    budget_max = request.budget_max or 10000.0
    budget_min = request.budget_min or budget_max * 0.6

    cypher = (
        "MATCH (r:Recipient {id: $rid})-[:HAS_TAG]->(t:Tag)-[:PREFERS_CATEGORY]->(c:Category) "
        "MATCH (p:Product)-[:BELONGS_TO]->(c) "
        "MATCH (c)-[:FIT_OCCASION]->(o:Occasion {code: $occasion}) "
        "WHERE p.price >= $bmin AND p.price <= $bmax "
        "WITH p, c, collect(DISTINCT t.name) AS matchedTags, count(DISTINCT t) AS tagScore "
        "ORDER BY tagScore DESC, p.salesCount DESC "
        "LIMIT 8 "
        "RETURN p.id AS id, p.name AS name, p.price AS price, "
        "p.platform AS platform, p.imageUrl AS image_url, "
        "matchedTags, tagScore, c.name AS category_name"
    )

    products = []
    related_tags = set()
    with _driver.session() as session:
        result = session.run(cypher, {
            "rid": request.recipient_id or 1,
            "occasion": occasion,
            "bmin": budget_min,
            "bmax": budget_max,
        })
        for record in result:
            tags = record["matchedTags"]
            related_tags.update(tags)
            tag_str = "\u3001".join(tags)
            cat_name = record["category_name"]
            prod_name = record["name"]
            occ_name = _OCCASION_NAMES.get(occasion, occasion)
            chain = (
                f"({tag_str})\u2192\u504f\u597d\u2192({cat_name})"
                f"\u2192\u5305\u542b\u2192({prod_name})\u2192\u9002\u5408\u2192({occ_name})"
            )
            products.append({
                "id": record["id"],
                "name": record["name"],
                "price": record["price"],
                "platform": record["platform"],
                "image_url": record["image_url"],
                "matched_tags": tags,
                "reasoning_chain": chain,
                "score": 0.85 + min(record["tagScore"] * 0.05, 0.15),
            })

    return KGQueryResponse(
        products=products,
        related_tags=list(related_tags),
        reasoning_chain="\n".join(p["reasoning_chain"] for p in products[:3]),
        neo4j_connected=True,
    )


@router.post("/kg/sync")
async def sync_from_taxonomy():
    """Build KG nodes and relationships from kg_taxonomy.json + trigger H2 sync."""
    if not _NEO4J_OK:
        return {"status": "error", "message": "Neo4j not connected"}

    taxonomy_path = Path(__file__).parent.parent / "data" / "kg_taxonomy.json"
    if not taxonomy_path.exists():
        return {"status": "error", "message": f"Taxonomy file not found: {taxonomy_path}"}

    with open(taxonomy_path, "r", encoding="utf-8") as f:
        tax = json.load(f)

    with _driver.session() as session:
        # Create constraints
        for label, prop in [("Tag", "name"), ("Category", "id"),
                            ("Product", "id"), ("Occasion", "code"),
                            ("Recipient", "id")]:
            session.run(f"CREATE CONSTRAINT IF NOT EXISTS FOR (n:{label}) REQUIRE n.{prop} IS UNIQUE")

        # Create Category nodes
        for cat in tax["standard_categories"]:
            session.run("MERGE (c:Category {id: $id}) SET c.name = $name",
                        id=cat["id"], name=cat["name"])

        # Create Occasion nodes
        for code, name in tax["occasion_codes"].items():
            session.run("MERGE (o:Occasion {code: $code}) SET o.name = $name",
                        code=code, name=name)

        # Tag -> Category
        for tag, cat_ids in tax["tag_to_categories"].items():
            for cat_id in cat_ids:
                session.run(
                    "MERGE (t:Tag {name: $tag}) "
                    "MERGE (c:Category {id: $cat}) "
                    "MERGE (t)-[:PREFERS_CATEGORY]->(c)",
                    tag=tag, cat=cat_id)

        # Category -> Occasion
        for cat_id, occs in tax["category_to_occasions"].items():
            for occ in occs:
                session.run(
                    "MERGE (c:Category {id: $cat}) "
                    "MERGE (o:Occasion {code: $occ}) "
                    "MERGE (c)-[:FIT_OCCASION]->(o)",
                    cat=cat_id, occ=occ)

    counts = {
        "categories": len(tax["standard_categories"]),
        "occasions": len(tax["occasion_codes"]),
        "tag_category_edges": sum(len(v) for v in tax["tag_to_categories"].values()),
        "category_occasion_edges": sum(len(v) for v in tax["category_to_occasions"].values()),
    }
    return {"status": "ok", "synced": counts}