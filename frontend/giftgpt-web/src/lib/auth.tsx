'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { setToken, getToken, authApi } from '@/lib/api';

interface User {
  id: number;
  nickname: string;
}

interface AuthContextType {
  user: User | null;
  login: (phone: string, password: string) => Promise<void>;
  register: (phone: string, password: string, nickname?: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const token = getToken();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({
          id: Number(payload.loginId) || 0,
          nickname: payload.nickname || ('用户' + String(payload.loginId || '').slice(-4)),
        });
      } catch {
        setUser({ id: 0, nickname: 'User' });
      }
    }
    setLoading(false);
  }, []);

  const login = async (phone: string, password: string) => {
    const res = await authApi.login(phone, password);
    setToken(res.token);
    setUser({ id: res.userId, nickname: res.nickname });
  };

  const register = async (phone: string, password: string, nickname?: string) => {
    const res = await authApi.register(phone, password, nickname);
    setToken(res.token);
    setUser({ id: res.userId, nickname: res.nickname });
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    router.push('/');
    authApi.logout().catch(() => {});
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated: !!user }}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
