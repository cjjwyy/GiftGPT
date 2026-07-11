'use client';

import { useState } from 'react';
import { Gift, Package } from 'lucide-react';
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
  recipientName?: string;
  recipientId?: number;
  occasion?: string;
}

export function GiftCard({ productId, productName, price, imageUrl, platform, platformUrl, reason, matchTags, score, recipientName, recipientId, occasion }: GiftCardProps) {
  const [imgError, setImgError] = useState(false);

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
          <span className="text-xs px-2 py-0.5 rounded-full bg-rose-50 text-rose-600 dark:bg-rose-900/30 dark:text-rose-400">
            {platform || '拼多多'}
          </span>
          {score !== undefined && score > 0 && (
            <span className="text-xs text-primary-600 bg-primary-50 dark:bg-primary-900/30 dark:text-primary-400 px-2 py-0.5 rounded-full">
              匹配 {(score * 100).toFixed(0)}%
            </span>
          )}
        </div>
        <h3 className="font-semibold text-gray-800 dark:text-gray-100 leading-snug line-clamp-2">{productName}</h3>
        <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 cursor-default" title={reason || '为TA精心挑选的礼物'}>{reason || '为TA精心挑选的礼物'}</p>
        {matchTags && matchTags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {matchTags.slice(0, 4).map(t => (
              <span key={t} className="text-xs bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400 px-2 py-0.5 rounded-full">{t}</span>
            ))}
          </div>
        )}
        <div className="flex items-center justify-between pt-2 border-t border-gray-50 dark:border-gray-800">
          <span className="text-xl font-bold text-rose-500">¥{price}</span>
          <div className="flex flex-col gap-2">
            <Link
              href={`/packaging?productName=${encodeURIComponent(productName)}&price=${price}${imageUrl ? `&imageUrl=${encodeURIComponent(imageUrl)}` : ''}${recipientId ? `&recipientId=${recipientId}&recipientName=${encodeURIComponent(recipientName || '')}&occasion=${encodeURIComponent(occasion || '')}` : ''}`}
              className="btn-outline text-sm py-1.5 px-3 flex items-center justify-center gap-1"
            >
              <Package className="w-3.5 h-3.5" /> 包装
            </Link>
            {productId > 0 ? (
              <Link href={`/products/${productId}`} className="btn-primary text-sm py-1.5 px-4 text-center">
                查看详情
              </Link>
            ) : platformUrl ? (
              <a href={platformUrl} target="_blank" rel="noopener noreferrer" className="btn-primary text-sm py-1.5 px-4 text-center">
                去购买
              </a>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}
