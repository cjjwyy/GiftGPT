'use client';

import { useState, useEffect } from 'react';
import { productApi } from '@/lib/api';
import { Product } from '@/types';
import { Loading } from '@/components/Loading';
import { Search, Gift } from 'lucide-react';
import Link from 'next/link';

export default function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('');

  useEffect(() => {
    productApi.search({}).then(d => { setProducts(d.records || []); setLoading(false); }).catch(() => setLoading(false));
  }, []);

  const onSearch = async () => {
    setLoading(true);
    const res = await productApi.search({ keyword, category });
    setProducts(res.records || []);
    setLoading(false);
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">商品选品</h1>
      <div className="card mb-8">
        <div className="flex gap-3">
          <input className="input-field flex-1" value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="搜索商品..." onKeyDown={e => e.key === 'Enter' && onSearch()} />
          <button onClick={onSearch} className="btn-primary flex items-center gap-2"><Search className="w-4 h-4" /> 搜索</button>
        </div>
      </div>
      {loading ? <Loading /> : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {products.map(p => (
            <Link key={p.id} href={`/products/${p.id}`} className="card hover:shadow-md transition-all">
              <div className="aspect-square bg-gradient-to-br from-gray-100 to-gray-200 rounded-xl mb-3 flex items-center justify-center">
                <Gift className="w-12 h-12 text-gray-400 dark:text-gray-500" />
              </div>
              <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-1">{p.name}</h3>
              <div className="flex items-center justify-between">
                <span className="text-lg font-bold text-rose-500">¥{p.price}</span>
                {p.platform && <span className="text-xs text-gray-400 dark:text-gray-500">{p.platform}</span>}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
