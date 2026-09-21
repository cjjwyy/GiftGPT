// 性格/兴趣标签统一词表。
// 前 22 个来自《GiftGPT知识图谱分类映射表》；后续为三人标注差异分析“修正框架”新增。
import vocabulary from './graphVocabulary.json';

export const TAG_DEFINITIONS = [...vocabulary.personality, ...vocabulary.interest];
export const TAG_OPTIONS = TAG_DEFINITIONS.map(item => item.value);
export const RELATION_OPTIONS = vocabulary.relation;
export function canonicalTag(tag: string): string {
  return TAG_DEFINITIONS.find(item => item.value === tag || item.aliases.includes(tag))?.value || tag;
}
export function canonicalRelation(relation: string): string {
  return RELATION_OPTIONS.find(item => item.value === relation || item.aliases.includes(relation))?.value || relation;
}

// 仅作为填写补充项时的提示示例，不预设哪些标签“有补充项”。
// 每个标签是否带补充项由用户在画像中选择。
export const TAG_SUPPLEMENT_EXAMPLES: Record<string, string[]> = {
  '音乐': ['吉他', '贝斯', '古典', 'Taylor Swift'],
  '运动': ['羽毛球', '健身', '跑步'],
};

export function parseSupplementText(text: string): string[] {
  return (text || '')
    .split(/[、,，;；]/)
    .map(item => item.trim())
    .filter(Boolean);
}

export function formatSupplementText(items: string[] | undefined): string {
  return (items || []).join('、');
}

export function buildTagSupplements(
  selectedTags: string[],
  supplementModes: Record<string, boolean>,
  supplementTexts: Record<string, string>
): Record<string, string[]> {
  const result: Record<string, string[]> = {};
  for (const tag of selectedTags) {
    if (!supplementModes[tag]) continue;
    const items = parseSupplementText(supplementTexts[tag] || '');
    if (items.length > 0) result[tag] = items;
  }
  return result;
}
