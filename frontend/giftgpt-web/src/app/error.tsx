'use client';

import { useEffect } from 'react';

export default function GlobalError({ error, reset }: { error: Error; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="max-w-xl mx-auto px-4 py-24 text-center">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">页面暂时无法加载</h1>
      <p className="text-gray-500 dark:text-gray-400 mt-2">可能是网络波动，请稍后重试。</p>
      <button type="button" onClick={reset} className="btn-primary mt-6">重新加载</button>
    </div>
  );
}
