'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import { useTheme } from '@/components/ThemeProvider';
import { Gift, Menu, X, Sun, Moon } from 'lucide-react';
import { useState } from 'react';

export function Navbar() {
  const { user, logout, isAuthenticated } = useAuth();
  const { theme, toggle } = useTheme();
  const [open, setOpen] = useState(false);

  const links = [
    { href: '/', label: '首页' },
    { href: '/recommend', label: '智能推荐' },
    { href: '/products', label: '选品' },
    { href: '/packaging', label: '包装' },
    { href: '/stories', label: '社区' },
    { href: '/calendar', label: '日历' },
  ];

  return (
    <nav className="sticky top-0 z-50 bg-white/75 dark:bg-gray-950/75 backdrop-blur-xl border-b border-gray-100/80 dark:border-gray-800/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl text-primary-600 dark:text-primary-400">
            <Gift className="w-7 h-7" />
            <span>GiftGPT</span>
          </Link>

          <div className="hidden md:flex items-center gap-1">
            {links.map(l => (
              <Link key={l.href} href={l.href} className="btn-ghost">{l.label}</Link>
            ))}
          </div>

          <div className="hidden md:flex items-center gap-3">
            <button
              onClick={toggle}
              className="p-2 rounded-lg text-gray-500 hover:text-primary-600 dark:text-gray-400 dark:hover:text-primary-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              title={theme === 'dark' ? '切换亮色模式' : '切换深色模式'}
            >
              {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
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

          <button className="md:hidden p-2 text-gray-600 dark:text-gray-300" onClick={() => setOpen(!open)}>
            {open ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {open && (
        <div className="md:hidden border-t border-gray-100 dark:border-gray-800 bg-white dark:bg-gray-900 px-4 py-3 space-y-2">
          {links.map(l => (
            <Link key={l.href} href={l.href} className="block py-2 text-gray-700 dark:text-gray-200" onClick={() => setOpen(false)}>
              {l.label}
            </Link>
          ))}
          <div className="flex items-center justify-between py-2">
            <span className="text-gray-700 dark:text-gray-200">
              {theme === 'dark' ? '深色模式' : '亮色模式'}
            </span>
            <button
              onClick={toggle}
              className="p-1.5 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
            >
              {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
          </div>
          {isAuthenticated ? (
            <>
              <Link href="/recipients" className="block py-2 text-gray-700 dark:text-gray-200">我的画像</Link>
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
