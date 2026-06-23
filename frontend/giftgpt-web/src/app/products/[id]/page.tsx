'use client';

import { useState, useEffect } from 'react';
import { productApi } from '@/lib/api';
import { useParams } from 'next/navigation';
import { Loading } from '@/components/Loading';
import { Gift, ExternalLink, Star, ShoppingBag } from 'lucide-react';

export default function ProductDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [product, setProduct] = useState<any>(null);
  const [imgError, setImgError] = useState(false);

  useEffect(() => {
    productApi.get(id).then(setProduct).catch(() => {});
  }, [id]);

  if (!product) return <Loading />;

  const platformColor = (p: string) => {
    switch (p) {
      case '京东': return 'bg-red-50 text-red-600';
      case '淘宝': return 'bg-orange-50 text-orange-600';
      case '拼多多': return 'bg-rose-50 text-rose-600';
      case '得物': return 'bg-blue-50 text-blue-600';
      default: return 'bg-gray-50 text-gray-600';
    }
  };

  const ratingStars = product.rating ? Math.round(product.rating) : 0;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="card grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Product image */}
        <div className="aspect-square bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-900 rounded-2xl flex items-center justify-center overflow-hidden">
          {product.imageUrl && !imgError ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover"
              onError={() => setImgError(true)}
            />
          ) : (
            <Gift className="w-24 h-24 text-gray-300 dark:text-gray-600" />
          )}
        </div>

        {/* Product info */}
        <div className="space-y-4">
          <div>
            {product.platform && (
              <span className={`text-xs px-2 py-0.5 rounded-full mb-2 inline-block ${platformColor(product.platform)}`}>
                {product.platform}
              </span>
            )}
            {product.category && (
              <span className="text-xs bg-primary-50 text-primary-600 px-2 py-0.5 rounded-full mb-2 ml-2 inline-block">
                {product.category}
              </span>
            )}
          </div>

          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{product.name}</h1>
          <p className="text-3xl font-bold text-rose-500">¥{product.price}</p>

          {/* Rating & sales */}
          <div className="flex items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
            {ratingStars > 0 && (
              <div className="flex items-center gap-1">
                {Array.from({ length: 5 }, (_, i) => (
                  <Star key={i} className={`w-4 h-4 ${i < ratingStars ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`} />
                ))}
                <span className="ml-1">{product.rating.toFixed(1)}</span>
              </div>
            )}
            {product.salesCount > 0 && (
              <span className="flex items-center gap-1">
                <ShoppingBag className="w-4 h-4" /> 已售 {product.salesCount.toLocaleString()}
              </span>
            )}
          </div>

          {product.description && (
            <p className="text-gray-600 dark:text-gray-300 leading-relaxed">{product.description}</p>
          )}

          <div className="flex gap-3 pt-4">
            {product.platformUrl ? (
              <a
                href={product.platformUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-primary inline-flex items-center gap-2"
              >
                立即购买 <ExternalLink className="w-4 h-4" />
              </a>
            ) : (
              <span className="text-sm text-gray-400 dark:text-gray-500 flex items-center gap-1">
                暂无购买链接
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
