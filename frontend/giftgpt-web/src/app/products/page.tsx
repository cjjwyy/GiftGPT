'use client';

import { useState, useEffect } from 'react';
import { productApi } from '@/lib/api';
import { Product } from '@/types';
import { Loading } from '@/components/Loading';
import { Search, Gift } from 'lucide-react';
import Link from 'next/link';

const PLATFORMS = [
  { value: '', label: '全部' },
  { value: '京东', label: '京东' },
  { value: '淘宝', label: '淘宝' },
  { value: '拼多多', label: '拼多多' },
  { value: '得物', label: '得物' },
];

export default function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [platform, setPlatform] = useState('');

  useEffect(() => {
    productApi.search({}).then(d => { setProducts(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const onSearch = async (p?: string) => {
    setLoading(true);
    const selectedPlatform = p !== undefined ? p : platform;
    const res = await productApi.search({ keyword, platform: selectedPlatform });
    setProducts(res.records || []);
    setLoading(false);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">商品选品</h1>

      <div className="card mb-4">
        <div className="flex gap-3">
          <input
            className="input-field flex-1"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            placeholder="搜索商品..."
            onKeyDown={e => e.key === 'Enter' && onSearch()}
          />
          <button onClick={() => onSearch()} className="btn-primary flex items-center gap-2">
            <Search className="w-4 h-4" /> 搜索
          </button>
        </div>
      </div>

      {/* Platform filter chips */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {PLATFORMS.map(pl => (
          <button
            key={pl.value}
            onClick={() => { setPlatform(pl.value); onSearch(pl.value); }}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
              platform === pl.value
                ? 'bg-primary-500 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700'
            }`}
          >
            {pl.label}
          </button>
        ))}
      </div>

      {loading ? <Loading /> : products.length === 0 ? (
        <div className="text-center py-20 text-gray-400 dark:text-gray-500">
          <Gift className="w-16 h-16 mx-auto mb-4" />
          <p className="text-lg">暂无商品</p>
          <p className="text-sm mt-1">尝试不同的搜索关键词或更换平台筛选</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {products.map(p => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}

function ProductCard({ product }: { product: Product }) {
  const [imgError, setImgError] = useState(false);

  const platformColor = (p: string) => {
    switch (p) {
      case '京东': return 'bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400';
      case '淘宝': return 'bg-orange-50 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400';
      case '拼多多': return 'bg-rose-50 text-rose-600 dark:bg-rose-900/30 dark:text-rose-400';
      case '得物': return 'bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400';
      default: return 'bg-gray-50 text-gray-600 dark:bg-gray-800 dark:text-gray-400';
    }
  };

  return (
    <Link href={`/products/${product.id}`} className="card hover:shadow-md transition-all group">
      <div className="aspect-square bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-900 rounded-xl mb-3 flex items-center justify-center overflow-hidden">
        {product.imageUrl && !imgError ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            referrerPolicy="no-referrer"
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            onError={() => setImgError(true)}
          />
        ) : (
          <Gift className="w-12 h-12 text-gray-300 dark:text-gray-600" />
        )}
      </div>

      {product.category && (
        <span className="text-xs bg-primary-50 text-primary-600 dark:bg-primary-900/30 dark:text-primary-400 px-2 py-0.5 rounded-full mb-2 inline-block">
          {product.category}
        </span>
      )}

      <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-1 line-clamp-2">{product.name}</h3>

      {product.description && (
        <p className="text-xs text-gray-400 dark:text-gray-500 line-clamp-1 mb-2">{product.description}</p>
      )}

      <div className="flex items-center justify-between mt-auto">
        <span className="text-lg font-bold text-rose-500">¥{product.price}</span>
        {product.platform && (
          <span className={`text-xs px-2 py-0.5 rounded-full ${platformColor(product.platform)}`}>
            {product.platform}
          </span>
        )}
      </div>

      {/* Rating & sales indicator */}
      {product.rating && product.rating > 0 && (
        <div className="flex items-center gap-2 mt-1.5 text-xs text-gray-400 dark:text-gray-500">
          <span>★ {product.rating.toFixed(1)}</span>
          {product.salesCount && product.salesCount > 0 && <span>已售 {product.salesCount.toLocaleString()}</span>}
        </div>
      )}
    </Link>
  );
}
