import vocabulary from './graphVocabulary.json';
export const OCCASIONS = vocabulary.occasion;

export type OccasionValue = (typeof OCCASIONS)[number]['value'];

export const OCCASION_LABELS: Record<string, string> = Object.fromEntries(
  OCCASIONS.map(item => [item.value, item.label]),
);
