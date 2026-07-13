'use client';

import { useState } from 'react';
import { Gift, Package, Share2 } from 'lucide-react';
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
  reasoningChain?: string;
}

function ReasoningChain({ chain }: { chain: string }) {
  const [open, setOpen] = useState(false);
  if (!chain) return null;
  const parts = chain.split('→').map(s => s.trim()).filter(Boolean);
  if (parts.length < 2) return null;
  return (
    <div className="mt-1">
      <button
        onClick={() => setOpen(v => !v)}
        className="text-xs text-primary-500 hover:text-primary-600 inline-flex items-center gap-1 cursor-pointer"
        type="button"
      >
        <Share2 className="w-3 h-3" /> 推理链 {open ? '▾' : '▸'}
      </button>
      {open && (
        <div className="flex items-center flex-wrap gap-1 mt-1 p-2 rounded-lg bg-gray-50 dark:bg-gray-800/50">
          {parts.map((part, i) => {
            const isRel = part.startsWith('[:') || part.length <= 4;
            const label = part.replace(/^\[:/, '').replace(/\]$/, '').replace(/^\(/, '').replace(/\)$/, '');
            return (
              <span key={i} className="inline-flex items-center gap-1">
                <span className={`text-xs px-1.5 py-0.5 rounded-full ${
                  isRel
                    ? 'bg-gray-200 dark:bg-gray-700 text-gray-400 italic'
                    : 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 font-medium'
                }`}>
                  {label}
                </span>
                {i < parts.length - 1 && <span className="text-gray-300 dark:text-gray-500 text-xs">→</span>}
              </span>
            );
          })}
        </div>
      )}
    </div>
  );
}

export function GiftCard({ productId, productName, price, imageUrl, platform, platformUrl, reason, matchTags, score, recipientName, recipientId, occasion, reasoningChain }: GiftCardProps) {
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
        {reasoningChain && <ReasoningChain chain={reasoningChain} />}
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
