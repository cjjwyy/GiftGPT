'use client';

import { useState, useEffect } from 'react';
import { productApi } from '@/lib/api';
import { useParams } from 'next/navigation';
import { Loading } from '@/components/Loading';
import { Gift, ExternalLink } from 'lucide-react';

export default function ProductDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [product, setProduct] = useState<any>(null);

  useEffect(() => {
    productApi.get(id).then(setProduct).catch(() => {});
  }, [id]);

  if (!product) return <Loading />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="card grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="aspect-square bg-gradient-to-br from-primary-50 to-rose-50 rounded-2xl flex items-center justify-center">
          <Gift className="w-24 h-24 text-primary-400" />
        </div>
        <div className="space-y-4">
          <h1 className="text-2xl font-bold text-gray-900">{product.name}</h1>
          <p className="text-3xl font-bold text-rose-500">¥{product.price}</p>
          {product.platform && (
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <span>平台: {product.platform}</span>
              {product.platformUrl && (
                <a href={product.platformUrl} target="_blank" rel="noopener" className="text-primary-500 inline-flex items-center gap-1">
                  去购买 <ExternalLink className="w-3 h-3" />
                </a>
              )}
            </div>
          )}
          {product.description && <p className="text-gray-600">{product.description}</p>}
          <div className="flex gap-3 pt-4">
            <a href={product.platformUrl || '#'} target="_blank" rel="noopener" className="btn-primary inline-flex items-center gap-2">
              立即购买 <ExternalLink className="w-4 h-4" />
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
