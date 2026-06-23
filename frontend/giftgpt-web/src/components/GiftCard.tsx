'use client';

import { useState } from 'react';
import { Gift } from 'lucide-react';
import Link from 'next/link';

interface GiftCardProps {
  productId: number;
  productName: string;
  price: number;
  imageUrl?: string;
  platform?: string;
  platformUrl?: string;
  reason: string;
  matchTags?: string[];
  score?: number;
}

export function GiftCard({ productId, productName, price, imageUrl, platform, platformUrl, reason, matchTags, score }: GiftCardProps) {
  const [imgError, setImgError] = useState(false);

  const platformColor = (p: string) => {
    switch (p) {
      case '京东': return 'bg-red-50 text-red-600';
      case '淘宝': return 'bg-orange-50 text-orange-600';
      case '拼多多': return 'bg-rose-50 text-rose-600';
      case '得物': return 'bg-blue-50 text-blue-600';
      default: return 'bg-gray-100 text-gray-500';
    }
  };

  return (
    <div className="card hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
      <div className="aspect-square bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-900 rounded-xl mb-4 flex items-center justify-center overflow-hidden">
        {imageUrl && !imgError ? (
          <img
            src={imageUrl}
            alt={productName}
            referrerPolicy="no-referrer"
            className="w-full h-full object-cover"
            onError={() => setImgError(true)}
          />
        ) : (
          <Gift className="w-16 h-16 text-gray-300 dark:text-gray-600" />
        )}
      </div>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className={`text-xs px-2 py-0.5 rounded-full ${platformColor(platform || '')}`}>
            {platform || '综合电商'}
          </span>
          {score !== undefined && score > 0 && (
            <span className="text-xs text-primary-600 bg-primary-50 dark:bg-primary-900/30 dark:text-primary-400 px-2 py-0.5 rounded-full">
              匹配 {(score * 100).toFixed(0)}%
            </span>
          )}
        </div>
        <h3 className="font-semibold text-gray-800 dark:text-gray-100 leading-snug line-clamp-2">{productName}</h3>
        <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-2">{reason}</p>
        {matchTags && matchTags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {matchTags.slice(0, 4).map(t => (
              <span key={t} className="text-xs bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400 px-2 py-0.5 rounded-full">{t}</span>
            ))}
          </div>
        )}
        <div className="flex items-center justify-between pt-2 border-t border-gray-50 dark:border-gray-800">
          <span className="text-xl font-bold text-rose-500">¥{price}</span>
          {productId > 0 ? (
            <Link href={`/products/${productId}`} className="btn-primary text-sm py-1.5 px-4">
              查看详情
            </Link>
          ) : platformUrl ? (
            <a href={platformUrl} target="_blank" rel="noopener noreferrer" className="btn-outline text-sm py-1.5 px-4">
              去购买
            </a>
          ) : (
            <span className="text-xs text-gray-400">暂无详情</span>
          )}
        </div>
      </div>
    </div>
  );
}
