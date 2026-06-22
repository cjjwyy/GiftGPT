'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { toast } from 'react-hot-toast';
import { Gift } from 'lucide-react';

export default function AuthPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, register } = useAuth();
  const router = useRouter();

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(phone, password);
      } else {
        await register(phone, password, nickname || undefined);
      }
      toast.success(mode === 'login' ? '登录成功' : '注册成功');
      router.push('/recommend');
    } catch (err: any) {
      toast.error(err.message || '操作失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[78vh] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-14 h-14 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-primary-500 to-rose-500 flex items-center justify-center shadow-lift">
            <Gift className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{mode === 'login' ? '欢迎回来' : '创建账号'}</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1.5 text-sm">{mode === 'login' ? '登录以继续使用' : '注册后即可开始使用AI选礼物'}</p>
        </div>

        <form onSubmit={onSubmit} className="card space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">手机号</label>
            <input className="input-field" type="text" value={phone} onChange={e => setPhone(e.target.value)} placeholder="请输入手机号" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">密码</label>
            <input className="input-field" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="请输入密码" required />
          </div>
          {mode === 'register' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">昵称 (选填)</label>
              <input className="input-field" type="text" value={nickname} onChange={e => setNickname(e.target.value)} placeholder="如何称呼你？" />
            </div>
          )}
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? '处理中...' : mode === 'login' ? '登录' : '注册'}
          </button>
        </form>

        <p className="text-center mt-5 text-sm text-gray-500 dark:text-gray-400">
          {mode === 'login' ? '还没有账号？' : '已有账号？'}
          <button onClick={() => setMode(mode === 'login' ? 'register' : 'login')} className="text-primary-600 dark:text-primary-400 ml-1 font-medium hover:underline">
            {mode === 'login' ? '立即注册' : '去登录'}
          </button>
        </p>
      </div>
    </div>
  );
}
