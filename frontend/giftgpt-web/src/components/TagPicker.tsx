'use client';

import {
  TAG_OPTIONS,
  TAG_SUPPLEMENT_EXAMPLES,
} from '@/lib/tagOptions';

interface TagPickerProps {
  selectedTags: string[];
  supplementModes: Record<string, boolean>;
  supplementTexts: Record<string, string>;
  onTagsChange: (tags: string[]) => void;
  onSupplementModeChange: (tag: string, hasSupplement: boolean) => void;
  onSupplementChange: (tag: string, value: string) => void;
}

export default function TagPicker({
  selectedTags,
  supplementModes,
  supplementTexts,
  onTagsChange,
  onSupplementModeChange,
  onSupplementChange,
}: TagPickerProps) {
  const toggleTag = (tag: string) => {
    const next = selectedTags.includes(tag)
      ? selectedTags.filter(t => t !== tag)
      : [...selectedTags, tag];
    onTagsChange(next);
  };

  return (
    <div className="space-y-4">
      <div>
        <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          选择标签（每个标签需标注是否有补充项）
        </p>
        <div className="flex flex-wrap gap-2">
          {TAG_OPTIONS.map(tag => (
            <button
              key={tag}
              type="button"
              className={selectedTags.includes(tag) ? 'tag-selected' : 'tag cursor-pointer hover:bg-primary-100'}
              onClick={() => toggleTag(tag)}
            >
              {tag}
            </button>
          ))}
        </div>
      </div>

      {selectedTags.length > 0 && (
        <div className="space-y-3 rounded-lg bg-primary-50/50 dark:bg-primary-900/20 p-3">
          <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
            请逐个标注已选标签是否有补充项
          </p>
          {selectedTags.map(tag => {
            const hasSupplement = !!supplementModes[tag];
            return (
              <div key={tag} className="space-y-1">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{tag}</span>
                  <button
                    type="button"
                    className={!hasSupplement ? 'tag-selected' : 'tag cursor-pointer hover:bg-primary-100'}
                    onClick={() => onSupplementModeChange(tag, false)}
                  >
                    无补充项
                  </button>
                  <button
                    type="button"
                    className={hasSupplement ? 'tag-selected' : 'tag cursor-pointer hover:bg-primary-100'}
                    onClick={() => onSupplementModeChange(tag, true)}
                  >
                    有补充项
                  </button>
                </div>
                {hasSupplement && (
                  <input
                    className="input-field"
                    value={supplementTexts[tag] || ''}
                    onChange={e => onSupplementChange(tag, e.target.value)}
                    placeholder={`如：${(TAG_SUPPLEMENT_EXAMPLES[tag] || ['吉他', '贝斯', '古典']).join('、')}（用顿号或逗号分隔）`}
                  />
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
