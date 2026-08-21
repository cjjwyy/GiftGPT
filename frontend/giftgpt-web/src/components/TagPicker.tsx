'use client';

import {
  TAG_OPTIONS,
  SUPPLEMENT_TAGS,
  TAG_SUPPLEMENT_EXAMPLES,
} from '@/lib/tagOptions';

interface TagPickerProps {
  selectedTags: string[];
  supplementTexts: Record<string, string>;
  onTagsChange: (tags: string[]) => void;
  onSupplementChange: (tag: string, value: string) => void;
}

export default function TagPicker({
  selectedTags,
  supplementTexts,
  onTagsChange,
  onSupplementChange,
}: TagPickerProps) {
  const supplementOptions = TAG_OPTIONS.filter(t => SUPPLEMENT_TAGS.includes(t));
  const normalOptions = TAG_OPTIONS.filter(t => !SUPPLEMENT_TAGS.includes(t));

  const toggleTag = (tag: string) => {
    const next = selectedTags.includes(tag)
      ? selectedTags.filter(t => t !== tag)
      : [...selectedTags, tag];
    onTagsChange(next);
  };

  const renderTagButton = (tag: string) => (
    <button
      key={tag}
      type="button"
      className={selectedTags.includes(tag) ? 'tag-selected' : 'tag cursor-pointer hover:bg-primary-100'}
      onClick={() => toggleTag(tag)}
    >
      {tag}
    </button>
  );

  const selectedSupplementTags = selectedTags.filter(t => SUPPLEMENT_TAGS.includes(t));

  return (
    <div className="space-y-4">
      <div>
        <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">有补充项</p>
        <div className="flex flex-wrap gap-2">
          {supplementOptions.map(renderTagButton)}
        </div>
      </div>

      <div>
        <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">无补充项</p>
        <div className="flex flex-wrap gap-2">
          {normalOptions.map(renderTagButton)}
        </div>
      </div>

      {selectedSupplementTags.length > 0 && (
        <div className="space-y-3 rounded-lg bg-primary-50/50 dark:bg-primary-900/20 p-3">
          <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
            请填写所选标签的补充项（必填）
          </p>
          {selectedSupplementTags.map(tag => (
            <div key={tag}>
              <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">{tag}</label>
              <input
                className="input-field"
                value={supplementTexts[tag] || ''}
                onChange={e => onSupplementChange(tag, e.target.value)}
                placeholder={`如：${TAG_SUPPLEMENT_EXAMPLES[tag].join('、')}（用顿号或逗号分隔）`}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
