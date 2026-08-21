// 性格/兴趣标签统一词表。
// 前 22 个来自《GiftGPT知识图谱分类映射表》；后续为三人标注差异分析“修正框架”新增。
export const TAG_OPTIONS = [
  '开朗', '文艺', '极客', '养生派', '摄影', '户外', '音乐', '运动',
  '美食', '咖啡', '旅行', '阅读', '动漫', '游戏', '宠物', '科技',
  '时尚', '简约', '复古', '浪漫', '艺术', '理性',
  '爱美', '感性', '品茶', '护肤', '抽烟', '条理', '热血', '自律', '贝斯',
  '与兴趣无关', '与性格无关',
];

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
