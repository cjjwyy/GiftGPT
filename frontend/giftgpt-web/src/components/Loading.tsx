'use client';

import { Loader2 } from 'lucide-react';

export function Loading({ text = '加载中...' }: { text?: string }) {
  return (
    <div className="flex items-center justify-center py-20">
      <Loader2 className="w-8 h-8 animate-spin text-primary-500 mr-3" />
      <span className="text-gray-500">{text}</span>
    </div>
  );
}
