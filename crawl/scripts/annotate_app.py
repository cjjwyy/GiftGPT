"""M3 标注工具: 逐条帖子关键词标注（Streamlit）

运行: streamlit run annotate_app.py    （在 scripts/ 目录下）

标注模型（每条帖子）:
  - 是否有价值: 有价值 / 无价值（无价值自动忽略，聚合时跳过）
  - 类列表（可增加/减少），每类:
      gender: 性别（唯一）
      relation: 关系（唯一）
      interest_tags: 兴趣标签（多个）
      personality_tags: 性格标签（多个）
      keywords: 关键词（多个，顿号/逗号分隔）
  - 自动产出四类映射: 性别→关键词 / 关系→关键词 / 兴趣标签→关键词 / 性格标签→关键词
    权重 = 该边被标注的出现次数

卡片（标签/性别/关系）:
  - 兴趣+性格标签（全量可选）/ 性别 / 关系
  - 标签、性别、关系可管理（删除/改名）

总览页:
  - 四类关键词映射表格（从帖子标注聚合，带权重）
  - 导出 kg_keywords.json（供原程序搜索按权重排序使用）
"""
import json
import os
import random
import re
import shutil
import sys
import time
import uuid
from collections import Counter, defaultdict

import streamlit as st

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _common

BASE = _common.BASE
SUMMARIES_PATH = os.path.join(BASE, "data", "summaries.jsonl")
POST_ANNOT_PATH = os.path.join(BASE, "data", "post_annotations.jsonl")
# 映射表路径自适应：先找本目录，找不到找上级（项目版在上级，桌面副本在本目录）
MAPPING_MD = os.path.join(BASE, "GiftGPT知识图谱分类映射表.md")
if not os.path.exists(MAPPING_MD):
    MAPPING_MD = os.path.join(BASE, "..", "GiftGPT知识图谱分类映射表.md")
CARDS_EXTRA_PATH = os.path.join(BASE, "data", "cards_extra.json")
CARDS_LOG_PATH = os.path.join(BASE, "data", "cards_changes.jsonl")
CARDS_REMOVED_PATH = os.path.join(BASE, "data", "cards_removed.json")
POST_ANNOT_BACKUP = os.path.join(BASE, "data", "post_annotations_backup.jsonl")

PAGE_SIZE = 5
RANDOM_SEED = 42

# ---------------- 显示名称与标签补充项 ----------------
# 标注工具内显示的标准品类名（只改显示，不影响品类 id 和底层映射表）
CATEGORY_DISPLAY_OVERRIDES = {
    "apparel": "服饰",
    "jewelry": "珠宝首饰",
}

# ---------------- 卡片（映射表） ----------------
def load_mapping_cards():
    """解析 GiftGPT知识图谱分类映射表.md:
    categories: {id: 中文名} / tags: [22] / opt_names: [500+] / opt_to_cat: {opt: cat}
    """
    cards = {"categories": {}, "tags": [], "opt_names": [], "opt_to_cat": {}}
    if not os.path.exists(MAPPING_MD):
        return cards
    try:
        text = open(MAPPING_MD, encoding="utf-8").read()
        for m in re.finditer(r"^\|\s*\d+\s*\|\s*([a-z_]+)\s*\|\s*([^|]+)\s*\|", text, re.M):
            cid = m.group(1)
            cname = m.group(2).strip()
            cards["categories"][cid] = CATEGORY_DISPLAY_OVERRIDES.get(cid, cname)
        in_tags = False
        for line in text.splitlines():
            if "兴趣标签" in line and "标准品类" in line:
                in_tags = True
                continue
            if in_tags:
                m = re.match(r"^\|\s*([^|]+?)\s*\|", line)
                if m:
                    tag_name = m.group(1).strip()
                    if tag_name and not tag_name.startswith("兴趣标签") and not re.match(r"^-+$", tag_name):
                        cards["tags"].append(tag_name)
                if line.startswith("## "):
                    break
        cur_cat = None
        for line in text.splitlines():
            m_cat = re.match(r"^###\s+.+?\(([a-z_]+)\)", line)
            if m_cat:
                cur_cat = m_cat.group(1)
                continue
            m_opt = re.match(r"^>\s*(.+)$", line)
            if m_opt and cur_cat:
                for name in re.split(r"[、，,；;]", m_opt.group(1)):
                    name = name.strip()
                    if name and name not in cards["opt_names"]:
                        cards["opt_names"].append(name)
                        cards["opt_to_cat"][name] = cur_cat
    except Exception as e:
        st.warning(f"映射表解析失败: {e}")
    return cards


# ---------------- 卡片扩展存储（标注中新增的条目） ----------------
def load_cards_extra():
    """新增卡片条目: {"categories": {...}, "tags": [...], "opt_names": [...], "opt_to_cat": {...},
    "genders": [...], "relations": [...]}"""
    default = {"categories": {}, "tags": [], "opt_names": [], "opt_to_cat": {}, "genders": [], "relations": []}
    if os.path.exists(CARDS_EXTRA_PATH):
        try:
            d = json.load(open(CARDS_EXTRA_PATH, encoding="utf-8"))
            for k in default:
                if k not in d:
                    d[k] = default[k]
            return d
        except Exception:
            pass
    return default


def save_cards_extra(d):
    tmp = CARDS_EXTRA_PATH + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
    os.replace(tmp, CARDS_EXTRA_PATH)


def log_card_change(action, etype, old, new=""):
    """记录卡片条目变更（添加/删除/改名）"""
    with open(CARDS_LOG_PATH, "a", encoding="utf-8") as f:
        f.write(json.dumps({"ts": int(time.time()), "action": action, "type": etype, "old": old, "new": new},
                           ensure_ascii=False) + "\n")


def load_card_changes():
    recs = []
    if os.path.exists(CARDS_LOG_PATH):
        for line in open(CARDS_LOG_PATH, encoding="utf-8"):
            line = line.strip()
            if not line:
                continue
            try:
                recs.append(json.loads(line))
            except Exception:
                continue
    return recs


def load_cards_removed():
    """被删除的卡片条目: {"categories": [...], "tags": [...], "opt_names": [...], "genders": [...], "relations": [...]}"""
    default = {"categories": [], "tags": [], "opt_names": [], "genders": [], "relations": []}
    if os.path.exists(CARDS_REMOVED_PATH):
        try:
            d = json.load(open(CARDS_REMOVED_PATH, encoding="utf-8"))
            for k in default:
                if k not in d:
                    d[k] = default[k]
            return d
        except Exception:
            pass
    return default


def save_cards_removed(d):
    tmp = CARDS_REMOVED_PATH + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
    os.replace(tmp, CARDS_REMOVED_PATH)


DEFAULT_GENDERS = ["男", "女", "不限"]
DEFAULT_RELATIONS = ["亲戚", "朋友", "同学", "同事", "恋人", "师生", "长辈", "晚辈", "子女", "其他"]


def load_all_cards():
    """映射表卡片 + 标注中新增的卡片 - 被删除的条目"""
    cards = load_mapping_cards()
    cards["genders"] = list(DEFAULT_GENDERS)
    cards["relations"] = list(DEFAULT_RELATIONS)
    extra = load_cards_extra()
    removed = load_cards_removed()
    cards["categories"].update(extra["categories"])
    cards["opt_to_cat"].update(extra["opt_to_cat"])
    for t in extra["tags"]:
        if t not in cards["tags"]:
            cards["tags"].append(t)
    for o in extra["opt_names"]:
        if o not in cards["opt_names"]:
            cards["opt_names"].append(o)
    for g in extra["genders"]:
        if g not in cards["genders"]:
            cards["genders"].append(g)
    for r in extra["relations"]:
        if r not in cards["relations"]:
            cards["relations"].append(r)
    # 应用删除
    for cid in removed["categories"]:
        cards["categories"].pop(cid, None)
        cards["opt_to_cat"] = {k: v for k, v in cards["opt_to_cat"].items() if v != cid}
    for t in removed["tags"]:
        if t in cards["tags"]:
            cards["tags"].remove(t)
    for o in removed["opt_names"]:
        if o in cards["opt_names"]:
            cards["opt_names"].remove(o)
        cards["opt_to_cat"].pop(o, None)
    for g in removed["genders"]:
        if g in cards["genders"]:
            cards["genders"].remove(g)
    for r in removed["relations"]:
        if r in cards["relations"]:
            cards["relations"].remove(r)
    return cards


def add_card_entry(etype, name, extra_info=""):
    """添加卡片条目并记录日志。extra_info: 品类用中文名 / opt_name 用所属品类 id"""
    d = load_cards_extra()
    if etype == "category":
        cat_id = name.strip()
        if cat_id and cat_id not in d["categories"] and cat_id not in load_mapping_cards()["categories"]:
            d["categories"][cat_id] = (extra_info or cat_id).strip()
            save_cards_extra(d)
            log_card_change("add", "category", cat_id, d["categories"][cat_id])
            return True
    elif etype == "tag":
        t = name.strip()
        if t and t not in d["tags"] and t not in load_mapping_cards()["tags"]:
            d["tags"].append(t)
            save_cards_extra(d)
            log_card_change("add", "tag", t)
            return True
    elif etype == "opt":
        o = name.strip()
        if o and o not in d["opt_names"] and o not in load_mapping_cards()["opt_names"]:
            d["opt_names"].append(o)
            if extra_info:
                d["opt_to_cat"][o] = extra_info.strip()
            save_cards_extra(d)
            log_card_change("add", "opt", o, extra_info.strip())
            return True
    elif etype in ("gender", "relation"):
        v = name.strip()
        if v and v not in d[f"{etype}s"]:
            d[f"{etype}s"].append(v)
            save_cards_extra(d)
            log_card_change("add", etype, v)
            return True
    return False


# ---------------- 帖子标注存储 ----------------
def load_summaries():
    recs = []
    if os.path.exists(SUMMARIES_PATH):
        for line in open(SUMMARIES_PATH, encoding="utf-8"):
            line = line.strip()
            if not line:
                continue
            try:
                recs.append(json.loads(line))
            except ValueError:
                continue
    return recs


def load_post_annotations():
    d = {}
    if os.path.exists(POST_ANNOT_PATH):
        for line in open(POST_ANNOT_PATH, encoding="utf-8"):
            line = line.strip()
            if not line:
                continue
            try:
                a = json.loads(line)
                d[a["id"]] = a
            except Exception:
                continue
    return d


def save_all_post_annotations(d):
    tmp = POST_ANNOT_PATH + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        for a in d.values():
            f.write(json.dumps(a, ensure_ascii=False) + "\n")
    os.replace(tmp, POST_ANNOT_PATH)


def save_post_annotation(annotation):
    d = load_post_annotations()
    d[annotation["id"]] = annotation
    save_all_post_annotations(d)


def delete_post_annotation(pid):
    d = load_post_annotations()
    if pid in d:
        del d[pid]
        save_all_post_annotations(d)


# ---------------- 匹配辅助 ----------------
def match_longest(item, words):
    best = None
    for w in words:
        if w and w in item:
            if best is None or len(w) > len(best):
                best = w
    return best


def render_add_field(etype, key, cat_for_opt=None):
    """在每个标注字段旁渲染紧凑的『搜索 + 添加』行。
    etype: category / tag / opt；opt 可指定所属品类（品类选择器 id）
    """
    label = {"category": "品类", "tag": "标签", "opt": "opt_name", "gender": "性别", "relation": "关系"}[etype]
    with st.container():
        col_s, col_a, col_b = st.columns([2, 2, 1])
        with col_s:
            st.caption(f"搜索{label}（在下方选择器中直接输入过滤）")
        with col_a:
            new_val = st.text_input(f"新{label}", key=f"addv_{key}", label_visibility="collapsed",
                                    placeholder=f"输入新{label}名称")
        with col_b:
            if st.button(f"➕ 添加{label}", key=f"addb_{key}"):
                if new_val.strip():
                    if etype == "opt" and cat_for_opt:
                        ok = add_card_entry("opt", new_val, cat_for_opt)
                    else:
                        ok = add_card_entry(etype, new_val)
                    if ok:
                        st.success(f"已添加 {new_val}")
                        st.rerun()
                    else:
                        st.warning("已存在或格式不正确")


def new_class_id():
    return uuid.uuid4().hex[:8]


def normalize_class(cls):
    """补齐旧标注缺失的字段，保证新旧标注都能渲染"""
    cls = dict(cls)
    if not cls.get("class_id"):
        cls["class_id"] = new_class_id()
    for key in ("interest_tags", "personality_tags", "keywords"):
        if not isinstance(cls.get(key), list):
            cls[key] = []
    for key in ("gender", "relation"):
        if not isinstance(cls.get(key), str):
            cls[key] = ""
    # 兼容旧数据：老字段直接丢弃，不参与新模型
    for old_key in ("category", "opt_names", "interest_tag_supplement_flags",
                    "personality_tag_supplement_flags", "interest_tag_supplements",
                    "personality_tag_supplements"):
        cls.pop(old_key, None)
    return cls


def split_keywords(text):
    """把标注者输入的关键词按空格/顿号/逗号/分号拆成列表"""
    return [x.strip() for x in re.split(r"[\s,，、;；]+", text or "") if x.strip()]


# ---------------- 页面 ----------------
st.set_page_config(page_title="KG 标注工具", page_icon="🏷️", layout="wide")

st.markdown(
    """
    <style>
    .go-bottom-btn {
        position: fixed; right: 16px; bottom: 24px; z-index: 9999;
        background: #ff4b4b; color: white; border: none; border-radius: 50%;
        width: 48px; height: 48px; font-size: 22px; cursor: pointer;
        box-shadow: 0 2px 8px rgba(0,0,0,.3);
    }
    .go-bottom-btn:hover { background: #e03e3e; }
    </style>
    <button class="go-bottom-btn" onclick="window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'})" title="快速到底部">⬇</button>
    """,
    unsafe_allow_html=True,
)

st.title("🏷️ KG 标注工具")
st.caption("逐条帖子细颗粒度标注 | 结果: data/post_annotations.jsonl")

page = st.sidebar.radio("导航", ["🏷️ 标注", "📋 总览"])

posts = load_summaries()
cards = load_all_cards()
cat_options = list(cards["categories"].keys())
tag_options = list(cards["tags"])
opt_options = list(cards["opt_names"])
opt_to_cat = cards["opt_to_cat"]
gender_options = list(cards["genders"])
relation_options = list(cards["relations"])


def format_category(cid):
    """品类选择器显示名；中文 id（如新增品类“烟”）避免显示成“烟 烟”"""
    name = cards["categories"].get(cid, cid)
    return name if name == cid else f"{cid} {name}"


def render_tag_picker(label, selected, prefix):
    """普通多选标签选择器；标签是否带补充项由下方逐标签标注"""
    return st.multiselect(f"{label}（可多选）", tag_options,
                          default=selected, key=f"{prefix}_tags")


def render_keyword_input(stored_keywords, prefix):
    """一个类的关键词输入框（多个用空格分隔，将用于搜索）"""
    value = " ".join(stored_keywords)
    entered = st.text_input(
        "关键词（多个用空格分隔，将用于搜索）",
        value=value,
        key=f"{prefix}_keywords",
        placeholder="如：吉他 礼物 贝斯 礼物 送女朋友礼物",
    )
    return split_keywords(entered)


def missing_keyword_fields(classes):
    """校验：有价值的每个类至少要有关键词（否则无法形成 节点→关键词 边）"""
    missing = []
    for i, cls in enumerate(classes, start=1):
        if not cls.get("keywords"):
            missing.append(f"类 {i} 未填写关键词")
    return missing


def make_default_classes(rec):
    """根据 AI 建议预填一个类（关键词来自 AI 提到的礼物项）"""
    ai_tags = rec.get("tags", [])
    ai_items = rec.get("items", [])
    matched_tags = [t for t in ai_tags if t in tag_options]
    return [{
        "class_id": new_class_id(),
        "gender": "",
        "relation": "",
        "interest_tags": list(matched_tags),
        "personality_tags": [],
        "keywords": list(ai_items),
    }]


def build_keyword_graph(post_annotations):
    """聚合所有有价值标注，生成 节点->关键词->权重（权重=标注出现次数）。
    返回: {"gender": {节点: {关键词: 次数}}, "relation": {...}, "tag": {...}}
    """
    kw_gender = Counter()
    kw_relation = Counter()
    kw_tag = Counter()
    for a in post_annotations.values():
        if not a.get("valuable"):
            continue
        for cls in a.get("classes", []):
            kws = [k.strip() for k in (cls.get("keywords") or []) if k and k.strip()]
            if not kws:
                continue
            if cls.get("gender"):
                for kw in kws:
                    kw_gender[(cls["gender"], kw)] += 1
            if cls.get("relation"):
                for kw in kws:
                    kw_relation[(cls["relation"], kw)] += 1
            for t in cls.get("interest_tags", []):
                for kw in kws:
                    kw_tag[(t, kw)] += 1
            for t in cls.get("personality_tags", []):
                for kw in kws:
                    kw_tag[(t, kw)] += 1

    def nest(counter):
        out = {}
        for (node, kw), cnt in counter.items():
            out.setdefault(node, {})[kw] = cnt
        return out

    return {"gender": nest(kw_gender), "relation": nest(kw_relation), "tag": nest(kw_tag)}


def save_keyword_graph(post_annotations, path=None):
    """把聚合后的关键词权重表写入 JSON，供原程序搜索按权重排序使用。"""
    graph = build_keyword_graph(post_annotations)
    if path is None:
        path = os.path.join(BASE, "data", "kg_keywords.json")
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(graph, f, ensure_ascii=False, indent=2)
    os.replace(tmp, path)
    return graph, path


# ============ 总览页 ============
if page == "📋 总览":
    st.subheader("📋 数据总览")
    post_annotations = load_post_annotations()

    total_posts = sum(1 for _ in open(SUMMARIES_PATH, encoding="utf-8") if _.strip())
    valuable = sum(1 for a in post_annotations.values() if a.get("valuable"))
    skipped = sum(1 for a in post_annotations.values() if not a.get("valuable"))
    c0, c1, c2, c3 = st.columns(4)
    c0.metric("帖子总数", total_posts)
    c1.metric("已标注(有价值)", valuable)
    c2.metric("已标注(无价值)", skipped)
    c3.metric("进度", f"{len(post_annotations) / max(total_posts, 1) * 100:.1f}%")

    import pandas as pd

    # ---- 关键词映射聚合（权重=标注出现次数） ----
    kw_gender = Counter()
    kw_relation = Counter()
    kw_interest = Counter()
    kw_personality = Counter()

    for a in post_annotations.values():
        if not a.get("valuable"):
            continue
        for cls in a.get("classes", []):
            kws = [k.strip() for k in (cls.get("keywords") or []) if k and k.strip()]
            if not kws:
                continue
            if cls.get("gender"):
                for kw in kws:
                    kw_gender[(cls["gender"], kw)] += 1
            if cls.get("relation"):
                for kw in kws:
                    kw_relation[(cls["relation"], kw)] += 1
            for t in cls.get("interest_tags", []):
                for kw in kws:
                    kw_interest[(t, kw)] += 1
            for t in cls.get("personality_tags", []):
                for kw in kws:
                    kw_personality[(t, kw)] += 1

    st.markdown("#### 关键词映射（权重 = 标注出现次数）")
    m_tabs = st.tabs([
        f"性别→关键词（{len(kw_gender)}）",
        f"关系→关键词（{len(kw_relation)}）",
        f"兴趣→关键词（{len(kw_interest)}）",
        f"性格→关键词（{len(kw_personality)}）",
    ])
    tab_cfgs = [
        (m_tabs[0], "性别", "关键词", kw_gender),
        (m_tabs[1], "关系", "关键词", kw_relation),
        (m_tabs[2], "兴趣标签", "关键词", kw_interest),
        (m_tabs[3], "性格标签", "关键词", kw_personality),
    ]
    for tab, node_label, kw_label, counter in tab_cfgs:
        with tab:
            if counter:
                st.dataframe(pd.DataFrame(
                    [{"节点": k[0], kw_label: k[1], "权重": v}
                     for k, v in counter.most_common()]),
                    use_container_width=True, height=400)
            else:
                st.info("暂无")

    st.markdown("---")
    col1, col2 = st.columns([1, 3])
    with col1:
        if st.button("📤 导出 kg_keywords.json", key="export_kw"):
            _, path = save_keyword_graph(post_annotations)
            st.success(f"已导出: {path}")
    with col2:
        st.caption("导出文件用于原程序搜索：按收礼人性别/关系/标签取关键词，权重越高的关键词越优先搜索。")

    # ---- 卡片条目管理（删除/改名，同步到帖子标注） ----
    st.markdown("#### 卡片条目管理")
    st.caption("点击表格行选中条目（下方操作直接作用于选中的条目）；删除/改名同步到所有帖子标注")

    def backup_post_annotations():
        """操作前备份帖子标注（撤销时恢复）"""
        if os.path.exists(POST_ANNOT_PATH):
            shutil.copy2(POST_ANNOT_PATH, POST_ANNOT_BACKUP)

    def sync_rename(entity_type, old, new):
        """实体改名：同步所有帖子标注 + 卡片，并记录日志（只支持 tag/gender/relation）"""
        backup_post_annotations()
        mapping = load_mapping_cards()
        extra = load_cards_extra()
        removed = load_cards_removed()
        if entity_type == "tag":
            if old in extra["tags"]:
                extra["tags"] = [new if x == old else x for x in extra["tags"]]
            elif old in removed["tags"]:
                removed["tags"] = [new if x == old else x for x in removed["tags"]]
            elif old in mapping["tags"]:
                if old not in removed["tags"]:
                    removed["tags"].append(old)
                if new not in extra["tags"]:
                    extra["tags"].append(new)
            save_cards_extra(extra)
            save_cards_removed(removed)
        elif entity_type in ("gender", "relation"):
            key = f"{entity_type}s"
            if old in extra[key]:
                extra[key] = [new if x == old else x for x in extra[key]]
            elif old in removed[key]:
                removed[key] = [new if x == old else x for x in removed[key]]
            elif old in cards[key]:
                if old not in removed[key]:
                    removed[key].append(old)
                if new not in extra[key]:
                    extra[key].append(new)
            save_cards_extra(extra)
            save_cards_removed(removed)
        else:
            return 0
        # 帖子标注同步
        d = load_post_annotations()
        changed = 0
        for a in d.values():
            for cls in a.get("classes", []):
                if entity_type == "tag":
                    if old in cls.get("interest_tags", []):
                        cls["interest_tags"] = [new if x == old else x for x in cls["interest_tags"]]
                        changed += 1
                    if old in cls.get("personality_tags", []):
                        cls["personality_tags"] = [new if x == old else x for x in cls["personality_tags"]]
                        changed += 1
                elif entity_type in ("gender", "relation") and cls.get(entity_type) == old:
                    cls[entity_type] = new
                    changed += 1
        save_all_post_annotations(d)
        log_card_change("rename", entity_type, old, new)
        return changed

    def sync_delete(entity_type, name):
        """删除实体：从卡片移除 + 同步清除所有帖子标注，并记录日志（只支持 tag/gender/relation）"""
        backup_post_annotations()
        extra = load_cards_extra()
        removed = load_cards_removed()
        if entity_type == "tag":
            if name in extra["tags"]:
                extra["tags"] = [x for x in extra["tags"] if x != name]
            else:
                if name not in removed["tags"]:
                    removed["tags"].append(name)
        elif entity_type in ("gender", "relation"):
            key = f"{entity_type}s"
            if name in extra[key]:
                extra[key] = [x for x in extra[key] if x != name]
            else:
                if name not in removed[key]:
                    removed[key].append(name)
        else:
            return 0
        save_cards_extra(extra)
        save_cards_removed(removed)
        # 帖子标注清理
        d = load_post_annotations()
        changed = 0
        for a in d.values():
            for cls in a.get("classes", []):
                if entity_type == "tag":
                    b1 = name in cls.get("interest_tags", [])
                    b2 = name in cls.get("personality_tags", [])
                    if b1:
                        cls["interest_tags"] = [x for x in cls["interest_tags"] if x != name]
                        changed += 1
                    if b2:
                        cls["personality_tags"] = [x for x in cls["personality_tags"] if x != name]
                        changed += 1
                if entity_type in ("gender", "relation") and cls.get(entity_type) == name:
                    cls[entity_type] = ""
                    changed += 1
        save_all_post_annotations(d)
        log_card_change("delete", entity_type, name)
        return changed

    def undo_all_changes():
        """撤销所有卡片更改：恢复帖子标注备份 + 清空卡片变更文件"""
        if os.path.exists(POST_ANNOT_BACKUP):
            shutil.copy2(POST_ANNOT_BACKUP, POST_ANNOT_PATH)
        for p in (CARDS_EXTRA_PATH, CARDS_REMOVED_PATH, CARDS_LOG_PATH, POST_ANNOT_BACKUP):
            if os.path.exists(p):
                os.remove(p)
        return True

    col_btn, col_undo, col_log = st.columns([1, 1, 3])
    with col_btn:
        if st.button("📜 更改记录"):
            st.session_state["show_changes"] = not st.session_state.get("show_changes", False)
    with col_undo:
        if st.button("⚠️ 撤销所有更改", type="secondary"):
            undo_all_changes()
            st.success("已撤销所有卡片更改（帖子标注恢复至最近一次操作前）")
            st.rerun()
    with col_log:
        if st.session_state.get("show_changes", False):
            changes = load_card_changes()
            if changes:
                st.dataframe(pd.DataFrame([{
                    "时间": c.get("ts", ""),
                    "操作": {"add": "添加", "delete": "删除", "rename": "改名"}.get(c.get("action"), c.get("action")),
                    "类型": c.get("type", ""),
                    "旧值": c.get("old", ""),
                    "新值": c.get("new", ""),
                } for c in reversed(changes)]), use_container_width=True, height=200)
            else:
                st.info("暂无变更记录")
    e_tabs = st.tabs([
        f"标签（{len(cards['tags'])}）",
        f"性别（{len(cards['genders'])}）",
        f"关系（{len(cards['relations'])}）",
    ])

    entity_conf = [
        ("标签", "tag", list(cards["tags"]), lambda c: c),
        ("性别", "gender", list(cards["genders"]), lambda c: c),
        ("关系", "relation", list(cards["relations"]), lambda c: c),
    ]
    for tab, (label, etype, items, fmt) in zip(e_tabs, entity_conf):
        with tab:
            search = st.text_input(f"搜索{label}", key=f"search_{etype}")
            shown = [i for i in items if not search or search in i]

            # 表格行点击选中（single-row），选中后同步到下方操作框
            sel_key = f"sel_{etype}"
            df = pd.DataFrame({label: [fmt(i) for i in shown]})
            event = st.dataframe(df, use_container_width=True, height=300,
                                 on_select="rerun", selection_mode="single-row",
                                 key=f"df_{etype}")
            if event and event.selection and event.selection.rows:
                row = event.selection.rows[0]
                if row < len(shown):
                    st.session_state[sel_key] = shown[row]

            current = st.session_state.get(sel_key, shown[0] if shown else "")
            if current not in shown:
                current = shown[0] if shown else ""
            st.write(f"**当前选中: {fmt(current)}**")
            st.caption("（点击上方表格行可切换选中条目，删除/改名直接作用于它）")

            r1, r2, r3 = st.columns([2, 1, 2])
            with r1:
                st.write("")  # 占位
            with r2:
                if st.button("🗑 删除", key=f"del_{etype}"):
                    n = sync_delete(etype, current)
                    st.success(f"已删除「{current}」，同步清理 {n} 处标注")
                    st.rerun()
            with r3:
                new_name = st.text_input("新名称", key=f"new_{etype}")
                if st.button("✏️ 改名", key=f"ren_{etype}"):
                    if new_name.strip() and new_name.strip() != current:
                        n = sync_rename(etype, current, new_name.strip())
                        st.success(f"已改名「{current}」→「{new_name.strip()}」，同步更新 {n} 处标注")
                        st.rerun()
    st.stop()

# ============ 标注页（逐条帖子） ============
post_annotations = load_post_annotations()
annotated_ids = set(post_annotations.keys())

# ---- 添加卡片条目（全局，对所有选择器生效） ----
with st.expander("➕ 添加卡片条目（标签/性别/关系）", expanded=False):
    add_t1, add_t2, add_t3 = st.tabs(["兴趣/性格标签", "性别", "关系"])
    with add_t1:
        c1, c2 = st.columns([3, 1])
        with c1:
            new_tag = st.text_input("新标签名称（兴趣或性格共用）", key="new_tag")
        with c2:
            if st.button("➕ 添加标签", key="add_tag_btn"):
                if new_tag.strip():
                    if add_card_entry("tag", new_tag):
                        st.success(f"已添加标签 {new_tag}")
                        st.rerun()
                    else:
                        st.warning("标签已存在")
    with add_t2:
        c1, c2 = st.columns([3, 1])
        with c1:
            new_gender = st.text_input("新性别（如：不限）", key="new_gender")
        with c2:
            if st.button("➕ 添加性别", key="add_gender_btn"):
                if new_gender.strip():
                    if add_card_entry("gender", new_gender):
                        st.success(f"已添加性别 {new_gender}")
                        st.rerun()
                    else:
                        st.warning("性别已存在")
    with add_t3:
        c1, c2 = st.columns([3, 1])
        with c1:
            new_relation = st.text_input("新关系（如：邻居）", key="new_relation")
        with c2:
            if st.button("➕ 添加关系", key="add_relation_btn"):
                if new_relation.strip():
                    if add_card_entry("relation", new_relation):
                        st.success(f"已添加关系 {new_relation}")
                        st.rerun()
                    else:
                        st.warning("关系已存在")

# 乱序（固定 seed）
if "post_order" not in st.session_state:
    order = list(range(len(posts)))
    random.Random(RANDOM_SEED).shuffle(order)
    st.session_state["post_order"] = order
order = st.session_state["post_order"]
shuffled = [posts[i] for i in order]

done = len(annotated_ids)
st.write(f"帖子总数: {len(posts)} | 已标注: {done} | 进度: {done / max(len(posts), 1) * 100:.1f}%")

if "post_page" not in st.session_state:
    st.session_state["post_page"] = 0
max_page = max(0, (len(shuffled) - 1) // PAGE_SIZE)
if st.session_state["post_page"] > max_page:
    st.session_state["post_page"] = max_page
pg = st.session_state["post_page"]
page_posts = shuffled[pg * PAGE_SIZE : (pg + 1) * PAGE_SIZE]

if len(shuffled) > PAGE_SIZE:
    pcol1, pcol2, pcol3, pcol4 = st.columns([1, 1, 2, 1])
    with pcol1:
        if st.button("◀ 上一页", key="prev_post", disabled=pg == 0):
            st.session_state["post_page"] -= 1
            st.rerun()
    with pcol2:
        st.write(f"第 {pg + 1} / {max_page + 1} 页")
    with pcol3:
        st.write(f"共 {len(shuffled)} 条帖子")
    with pcol4:
        if st.button("下一页 ▶", key="next_post", disabled=pg >= max_page):
            st.session_state["post_page"] += 1
            st.rerun()

for p in page_posts:
    pid = p.get("id", "")
    ann = post_annotations.get(pid, {})
    is_done = pid in annotated_ids

    with st.container(border=True):
        c_top1, c_top2 = st.columns([6, 1])
        with c_top1:
            st.markdown(f"**{p.get('title', pid)}**")
        with c_top2:
            if is_done:
                st.markdown("🟢 有价值" if ann.get("valuable") else "⚪ 无价值")
            else:
                st.markdown("⚪ 未标")

        meta = []
        if p.get("post_time"):
            meta.append(f"🕐 {p['post_time']}")
        if p.get("url"):
            meta.append(f"🔗 {p['url']}")
        if meta:
            st.caption(" | ".join(meta))

        with st.expander("查看正文与 AI 建议", expanded=False):
            if p.get("content"):
                c = p["content"]
                st.markdown(c[:800] + ("..." if len(c) > 800 else ""))
            st.markdown("---")
            st.markdown("**🤖 AI 建议（参考）**")
            st.markdown(
                f"标签: {', '.join(p.get('tags', [])) or '—'} ｜ "
                f"礼物: {', '.join(p.get('items', [])) or '—'} ｜ "
                f"场景: {p.get('occasion') or '—'}"
            )

        # 有价值标注
        val_key = f"val_{pid}"
        default_val = ann.get("valuable", True)
        valuable = st.radio("是否有价值", ["有价值", "无价值"], index=0 if default_val else 1,
                            key=val_key, horizontal=True)

        # 类列表（session_state 动态增删）
        classes_key = f"classes_{pid}"
        if classes_key not in st.session_state:
            if is_done and ann.get("classes"):
                st.session_state[classes_key] = [normalize_class(c) for c in json.loads(json.dumps(ann["classes"]))]
            else:
                st.session_state[classes_key] = make_default_classes(p)
        classes = st.session_state[classes_key]

        if valuable == "有价值":
            st.markdown(f"**类列表（{len(classes)} 个，每类选节点 + 填关键词）**")
            del_ids = []
            for display_i, cls in enumerate(classes, start=1):
                class_id = cls.get("class_id") or new_class_id()
                cls["class_id"] = class_id
                with st.container(border=True):
                    head_l, head_r = st.columns([6, 1])
                    with head_l:
                        st.markdown(f"**类 {display_i}**")
                    with head_r:
                        if len(classes) > 1 and st.button("🗑 删类", key=f"delc_{pid}_{class_id}"):
                            del_ids.append(class_id)
                    gr1, gr2 = st.columns([1, 1])
                    with gr1:
                        gender = st.selectbox(
                            "性别（唯一）",
                            gender_options,
                            index=gender_options.index(cls["gender"]) if cls.get("gender") in gender_options else 0,
                            key=f"gender_{pid}_{class_id}",
                        )
                        render_add_field("gender", f"gender_{pid}_{class_id}")
                    with gr2:
                        relation = st.selectbox(
                            "关系（唯一）",
                            relation_options,
                            index=relation_options.index(cls["relation"]) if cls.get("relation") in relation_options else 0,
                            key=f"relation_{pid}_{class_id}",
                        )
                        render_add_field("relation", f"relation_{pid}_{class_id}")
                    t1, t2 = st.columns([1, 1])
                    with t1:
                        itags = render_tag_picker("兴趣标签", cls.get("interest_tags", []),
                                                  f"it_{pid}_{class_id}")
                        render_add_field("tag", f"it_{pid}_{class_id}")
                    with t2:
                        ptags = render_tag_picker("性格标签", cls.get("personality_tags", []),
                                                  f"pt_{pid}_{class_id}")
                        render_add_field("tag", f"pt_{pid}_{class_id}")
                    keywords = render_keyword_input(cls.get("keywords", []), f"kw_{pid}_{class_id}")
                    cls.update({"gender": gender, "relation": relation,
                                "interest_tags": itags, "personality_tags": ptags,
                                "keywords": keywords})
            if del_ids:
                del_set = set(del_ids)
                classes[:] = [c for c in classes if c.get("class_id") not in del_set]
                st.rerun()
            if st.button("➕ 添加类", key=f"addc_{pid}"):
                classes.append({"class_id": new_class_id(), "gender": "", "relation": "",
                                "interest_tags": [], "personality_tags": [], "keywords": []})
                st.rerun()

        missing_kws = missing_keyword_fields(classes) if valuable == "有价值" else []
        if missing_kws:
            st.error("请先填写关键词：" + "、".join(missing_kws))

        b1, b2, b3 = st.columns([1, 1, 1])
        with b1:
            if st.button("💾 保存", key=f"save_{pid}", disabled=bool(missing_kws)):
                payload = {
                    "id": pid,
                    "valuable": valuable == "有价值",
                    "classes": [] if valuable != "有价值" else json.loads(json.dumps(classes)),
                    "ts": int(time.time()),
                }
                save_post_annotation(payload)
                st.rerun()
        with b2:
            if is_done and st.button("↩️ 撤回", key=f"undo_{pid}"):
                if classes_key in st.session_state:
                    del st.session_state[classes_key]
                delete_post_annotation(pid)
                st.rerun()
