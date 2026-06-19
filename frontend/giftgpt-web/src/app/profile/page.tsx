'use client';

import { useAuth } from '@/lib/auth';
import Link from 'next/link';
import { User, Gift, Calendar, Building2 } from 'lucide-react';

export default function ProfilePage() {
  const { user, logout, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <User className="w-20 h-20 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-gray-700 dark:text-gray-300 mb-2">请先登录</h2>
        <Link href="/auth" className="btn-primary">去登录</Link>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-10">
      <div className="card text-center mb-6">
        <div className="w-20 h-20 bg-gradient-to-br from-primary-400 to-rose-400 rounded-full flex items-center justify-center text-white text-2xl font-bold mx-auto mb-3">
          {user.nickname?.[0] || 'U'}
        </div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">{user.nickname}</h2>
      </div>

      <div className="space-y-3">
        <Link href="/recipients" className="card flex items-center gap-4 hover:shadow-md transition-all">
          <User className="w-6 h-6 text-primary-500" />
          <div><p className="font-medium">收礼人画像管理</p><p className="text-sm text-gray-400 dark:text-gray-500">管理你的送礼对象</p></div>
        </Link>
        <Link href="/gifts" className="card flex items-center gap-4 hover:shadow-md transition-all">
          <Gift className="w-6 h-6 text-rose-500" />
          <div><p className="font-medium">礼物记忆库</p><p className="text-sm text-gray-400 dark:text-gray-500">查看历史送礼记录</p></div>
        </Link>
        <Link href="/calendar" className="card flex items-center gap-4 hover:shadow-md transition-all">
          <Calendar className="w-6 h-6 text-blue-500" />
          <div><p className="font-medium">日历提醒</p><p className="text-sm text-gray-400 dark:text-gray-500">重要日子不错过</p></div>
        </Link>
        <Link href="/enterprise" className="card flex items-center gap-4 hover:shadow-md transition-all">
          <Building2 className="w-6 h-6 text-purple-500" />
          <div><p className="font-medium">企业服务</p><p className="text-sm text-gray-400 dark:text-gray-500">团购福利、员工关怀</p></div>
        </Link>
        <button onClick={logout} className="btn-outline w-full mt-4">退出登录</button>
      </div>
    </div>
  );
}
