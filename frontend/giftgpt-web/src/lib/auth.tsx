'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
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

  useEffect(() => {
    const token = getToken();
    if (token) {
      setUser({ id: 0, nickname: 'User' });
    }
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
    authApi.logout().catch(() => {});
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
