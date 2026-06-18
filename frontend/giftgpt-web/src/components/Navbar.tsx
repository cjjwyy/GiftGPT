'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import { Gift, Menu, X } from 'lucide-react';
import { useState } from 'react';

export function Navbar() {
  const { user, logout, isAuthenticated } = useAuth();
  const [open, setOpen] = useState(false);

  const links = [
    { href: '/', label: '首页' },
    { href: '/recommend', label: '智能推荐' },
    { href: '/products', label: '选品' },
    { href: '/stories', label: '社区' },
  ];

  return (
    <nav className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl text-primary-600">
            <Gift className="w-7 h-7" />
            <span>GiftGPT</span>
          </Link>

          <div className="hidden md:flex items-center gap-1">
            {links.map(l => (
              <Link key={l.href} href={l.href} className="btn-ghost">{l.label}</Link>
            ))}
          </div>

          <div className="hidden md:flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <Link href="/recipients" className="btn-ghost">我的画像</Link>
                <Link href="/profile" className="btn-ghost">{user?.nickname}</Link>
                <button onClick={logout} className="btn-outline text-sm py-1.5 px-4">退出</button>
              </>
            ) : (
              <Link href="/auth" className="btn-primary text-sm py-1.5 px-5">登录</Link>
            )}
          </div>

          <button className="md:hidden p-2" onClick={() => setOpen(!open)}>
            {open ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {open && (
        <div className="md:hidden border-t bg-white px-4 py-3 space-y-2">
          {links.map(l => (
            <Link key={l.href} href={l.href} className="block py-2 text-gray-700" onClick={() => setOpen(false)}>
              {l.label}
            </Link>
          ))}
          {isAuthenticated ? (
            <>
              <Link href="/recipients" className="block py-2 text-gray-700">我的画像</Link>
              <button onClick={() => { logout(); setOpen(false); }} className="btn-outline w-full">退出</button>
            </>
          ) : (
            <Link href="/auth" className="btn-primary block text-center" onClick={() => setOpen(false)}>登录</Link>
          )}
        </div>
      )}
    </nav>
  );
}
