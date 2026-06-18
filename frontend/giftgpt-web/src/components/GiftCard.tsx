'use client';

import { Gift, Heart, Tag } from 'lucide-react';
import Link from 'next/link';

interface GiftCardProps {
  productId: number;
  productName: string;
  price: number;
  imageUrl?: string;
  platform?: string;
  reason: string;
  matchTags?: string[];
  score?: number;
}

export function GiftCard({ productId, productName, price, imageUrl, platform, reason, matchTags, score }: GiftCardProps) {
  return (
    <div className="card hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
      <div className="aspect-square bg-gradient-to-br from-primary-50 to-rose-50 rounded-xl mb-4 flex items-center justify-center">
        <Gift className="w-16 h-16 text-primary-400" />
      </div>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">{platform || '平台直购'}</span>
          {score && (
            <span className="text-xs text-primary-600 bg-primary-50 px-2 py-0.5 rounded-full">
              匹配 {(score * 100).toFixed(0)}%
            </span>
          )}
        </div>
        <h3 className="font-semibold text-gray-800 leading-snug">{productName}</h3>
        <p className="text-sm text-gray-500 line-clamp-2">{reason}</p>
        {matchTags && matchTags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {matchTags.map(t => (
              <span key={t} className="text-xs bg-primary-50 text-primary-600 px-2 py-0.5 rounded-full">{t}</span>
            ))}
          </div>
        )}
        <div className="flex items-center justify-between pt-2 border-t border-gray-50">
          <span className="text-xl font-bold text-rose-500">¥{price}</span>
          <Link href={`/products/${productId}`} className="btn-primary text-sm py-1.5 px-4">查看详情</Link>
        </div>
      </div>
    </div>
  );
}
