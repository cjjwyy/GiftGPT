import type { ReactNode } from 'react';

export function EmptyState({ title, description, action }: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="card max-w-2xl mx-auto py-16 px-6 text-center text-gray-400 dark:text-gray-500">
      <p className="text-lg text-gray-600 dark:text-gray-300">{title}</p>
      {description && <p className="text-sm mt-2">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
