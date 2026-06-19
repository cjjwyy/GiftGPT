import type { Metadata } from 'next';
import './globals.css';
import { Providers } from './providers';
import { Navbar } from '@/components/Navbar';
import { Toaster } from '@/components/Toaster';

export const metadata: Metadata = {
  title: 'GiftGPT — AI 驱动的礼物推荐平台',
  description: '不猜，更懂TA的心意。消除"不知道送什么"的焦虑，让每一份礼物都恰到好处。',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body className="min-h-screen flex flex-col bg-gray-50 text-gray-900 antialiased dark:bg-gray-950 dark:text-gray-100">
        <Providers>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Toaster />
        </Providers>
      </body>
    </html>
  );
}
