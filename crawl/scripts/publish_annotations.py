"""Validate, aggregate and publish completed human annotations.

This script never invents labels. It only consumes records already decided by a
human in post_annotations.jsonl and turns them into the runtime kg_keywords.json.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import tempfile
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

CRAWL_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = CRAWL_ROOT.parent
DEFAULT_INPUT = CRAWL_ROOT / "data" / "post_annotations.jsonl"
DEFAULT_OUTPUT = CRAWL_ROOT / "data" / "kg_keywords.json"
DEFAULT_RUNTIME = (
    PROJECT_ROOT
    / "backend"
    / "giftgpt-recommendation"
    / "src"
    / "main"
    / "resources"
    / "kg_keywords.json"
)
GRAPH_KEYS = ("gender", "relation", "occasion", "tag")


def read_jsonl(paths: Iterable[Path]) -> tuple[dict[str, dict[str, Any]], list[str]]:
    """Return the newest valid record per id and validation warnings."""
    records: dict[str, dict[str, Any]] = {}
    versions: dict[str, tuple[int, int]] = {}
    warnings: list[str] = []
    sequence = 0
    for path in paths:
        if not path.exists():
            warnings.append(f"{path}: 文件不存在")
            continue
        with path.open(encoding="utf-8") as source:
            for line_number, line in enumerate(source, start=1):
                if not line.strip():
                    continue
                sequence += 1
                try:
                    record = json.loads(line)
                except json.JSONDecodeError as exc:
                    warnings.append(f"{path}:{line_number}: JSON 无效（{exc.msg}）")
                    continue
                record_id = clean_text(record.get("id"), 200)
                if not record_id:
                    warnings.append(f"{path}:{line_number}: 缺少 id，已跳过")
                    continue
                timestamp = safe_int(record.get("ts"))
                version = (timestamp, sequence)
                if version >= versions.get(record_id, (-1, -1)):
                    versions[record_id] = version
                    records[record_id] = record
    return records, warnings


def build_graph(records: Iterable[dict[str, Any]], min_weight: int = 1) -> dict[str, Any]:
    counters = {key: Counter() for key in GRAPH_KEYS}
    for record in records:
        if record.get("valuable") is not True:
            continue
        classes = record.get("classes")
        if not isinstance(classes, list):
            continue
        for annotation_class in classes:
            if not isinstance(annotation_class, dict):
                continue
            keywords = unique_clean_strings(annotation_class.get("keywords"), 100)
            if not keywords:
                continue
            nodes = {
                "gender": unique_clean_strings([annotation_class.get("gender")], 50),
                "relation": unique_clean_strings([annotation_class.get("relation")], 50),
                "occasion": unique_clean_strings([annotation_class.get("occasion")], 50),
                "tag": unique_clean_strings(
                    list_value(annotation_class.get("interest_tags"))
                    + list_value(annotation_class.get("personality_tags")),
                    50,
                ),
            }
            for graph_key, graph_nodes in nodes.items():
                for node in graph_nodes:
                    for keyword in keywords:
                        counters[graph_key][(node, keyword)] += 1

    graph: dict[str, dict[str, dict[str, int]]] = {key: {} for key in GRAPH_KEYS}
    threshold = max(1, min_weight)
    for graph_key, counter in counters.items():
        for (node, keyword), weight in sorted(
            counter.items(), key=lambda item: (item[0][0], -item[1], item[0][1])
        ):
            if weight >= threshold:
                graph[graph_key].setdefault(node, {})[keyword] = weight
    return graph


def validate_graph(graph: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if set(graph) != set(GRAPH_KEYS):
        errors.append("输出必须且只能包含 gender/relation/occasion/tag")
    for graph_key in GRAPH_KEYS:
        nodes = graph.get(graph_key)
        if not isinstance(nodes, dict):
            errors.append(f"{graph_key} 必须是对象")
            continue
        for node, keywords in nodes.items():
            if not clean_text(node, 50) or not isinstance(keywords, dict):
                errors.append(f"{graph_key} 包含无效节点")
                continue
            for keyword, weight in keywords.items():
                if not clean_text(keyword, 100) or not isinstance(weight, int) or weight < 1:
                    errors.append(f"{graph_key}.{node} 包含无效关键词或权重")
    return errors


def atomic_write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    handle, temp_name = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(handle, "w", encoding="utf-8", newline="\n") as target:
            json.dump(payload, target, ensure_ascii=False, indent=2)
            target.write("\n")
            target.flush()
            os.fsync(target.fileno())
        os.replace(temp_name, path)
    except Exception:
        try:
            os.unlink(temp_name)
        except FileNotFoundError:
            pass
        raise


def publish(output: Path, runtime_path: Path) -> None:
    runtime_path.parent.mkdir(parents=True, exist_ok=True)
    if runtime_path.exists():
        shutil.copy2(runtime_path, runtime_path.with_suffix(runtime_path.suffix + ".bak"))
    payload = json.loads(output.read_text(encoding="utf-8"))
    atomic_write_json(runtime_path, payload)


def graph_edge_count(graph: dict[str, Any]) -> int:
    return sum(
        len(keywords)
        for graph_key in GRAPH_KEYS
        for keywords in graph.get(graph_key, {}).values()
    )


def list_value(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def unique_clean_strings(values: Any, max_length: int) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in list_value(values):
        cleaned = clean_text(value, max_length)
        if cleaned and cleaned not in seen:
            seen.add(cleaned)
            result.append(cleaned)
    return result


def clean_text(value: Any, max_length: int) -> str:
    if not isinstance(value, str):
        return ""
    return " ".join(value.split()).strip()[:max_length]


def safe_int(value: Any) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="聚合并发布已完成人工标注的数据")
    parser.add_argument("inputs", nargs="*", type=Path, default=[DEFAULT_INPUT])
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--min-weight", type=int, default=1)
    parser.add_argument("--publish", action="store_true", help="原子发布到后端运行资源")
    parser.add_argument("--runtime-path", type=Path, default=DEFAULT_RUNTIME)
    parser.add_argument("--allow-empty", action="store_true", help="允许生成空图（默认拒绝，防止误覆盖）")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    records, warnings = read_jsonl(args.inputs)
    for warning in warnings:
        print("WARN:", warning)
    graph = build_graph(records.values(), args.min_weight)
    errors = validate_graph(graph)
    if errors:
        for error in errors:
            print("ERROR:", error)
        return 2
    edges = graph_edge_count(graph)
    if edges == 0 and not args.allow_empty:
        print("ERROR: 没有可发布的已标注关系；未覆盖现有运行数据")
        return 3
    atomic_write_json(args.output, graph)
    print(f"已聚合 {len(records)} 条去重标注、{edges} 条加权关系 -> {args.output}")
    if args.publish:
        publish(args.output, args.runtime_path)
        print(f"已原子发布 -> {args.runtime_path}（旧文件保留为 .bak）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
