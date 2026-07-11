'use client';

import { useState, useEffect } from 'react';
import { productApi } from '@/lib/api';
import { Product } from '@/types';
import { Loading } from '@/components/Loading';
import { Search, Gift, Package } from 'lucide-react';
import Link from 'next/link';

export default function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    const saved = sessionStorage.getItem('lastProductSearch');
    if (saved) {
      try {
        const data = JSON.parse(saved);
        setKeyword(data.keyword || '');
        setProducts(data.products || []);
        setLoading(false);
        return;
      } catch {}
    }
    productApi.search({ keyword: '礼物' }).then(d => { setProducts(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const onSearch = async () => {
    setLoading(true);
    const res = await productApi.search({ keyword });
    const records = res.records || [];
    setProducts(records);
    sessionStorage.setItem('lastProductSearch', JSON.stringify({ keyword, products: records }));
    setLoading(false);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">商品选品</h1>

      <div className="card mb-6">
        <div className="flex gap-3">
          <input
            className="input-field flex-1"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            placeholder="搜索拼多多商品..."
            onKeyDown={e => e.key === 'Enter' && onSearch()}
          />
          <button onClick={() => onSearch()} className="btn-primary flex items-center gap-2">
            <Search className="w-4 h-4" /> 搜索
          </button>
        </div>
      </div>

      {loading ? <Loading /> : products.length === 0 ? (
        <div className="text-center py-20 text-gray-400 dark:text-gray-500">
          <Gift className="w-16 h-16 mx-auto mb-4" />
          <p className="text-lg">暂无商品</p>
          <p className="text-sm mt-1">尝试不同的搜索关键词</p>
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
        <Link
          href={`/packaging?productName=${encodeURIComponent(product.name)}&price=${product.price}${product.imageUrl ? `&imageUrl=${encodeURIComponent(product.imageUrl)}` : ''}${product.id ? `&productId=${product.id}` : ''}`}
          className="btn-outline text-sm py-1.5 px-3 flex items-center gap-1"
        >
          <Package className="w-3.5 h-3.5" /> 包装
        </Link>
      </div>

      {(product.rating ?? 0) > 0 && (
        <div className="flex items-center gap-2 mt-1.5 text-xs text-gray-400 dark:text-gray-500">
          <span>★ {product.rating!.toFixed(1)}</span>
          {product.salesCount && product.salesCount > 0 && <span>已售 {product.salesCount.toLocaleString()}</span>}
        </div>
      )}
    </Link>
  );
}
