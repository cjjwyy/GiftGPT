import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "publish_annotations.py"
SPEC = importlib.util.spec_from_file_location("publish_annotations", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class PublishAnnotationsTest(unittest.TestCase):
    def test_latest_record_wins_and_duplicate_keywords_do_not_inflate(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "annotations.jsonl"
            records = [
                {"id": "p1", "ts": 1, "valuable": False, "classes": []},
                {
                    "id": "p1",
                    "ts": 2,
                    "valuable": True,
                    "classes": [{
                        "gender": "女",
                        "relation": "朋友",
                        "occasion": "birthday",
                        "interest_tags": ["阅读"],
                        "personality_tags": ["文艺", "阅读"],
                        "keywords": ["书", "书", " 钢笔 "],
                    }],
                },
            ]
            path.write_text("\n".join(json.dumps(item, ensure_ascii=False) for item in records), encoding="utf-8")
            loaded, warnings = MODULE.read_jsonl([path])
            graph = MODULE.build_graph(loaded.values())

            self.assertEqual([], warnings)
            self.assertEqual(1, graph["gender"]["女"]["书"])
            self.assertEqual(1, graph["tag"]["阅读"]["钢笔"])
            self.assertEqual(1, graph["occasion"]["birthday"]["书"])

    def test_empty_graph_is_structurally_valid(self):
        graph = MODULE.build_graph([])
        self.assertEqual([], MODULE.validate_graph(graph))
        self.assertEqual(0, MODULE.graph_edge_count(graph))


if __name__ == "__main__":
    unittest.main()
