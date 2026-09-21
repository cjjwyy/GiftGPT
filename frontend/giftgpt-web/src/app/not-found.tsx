import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="max-w-xl mx-auto px-4 py-24 text-center">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">没有找到这个页面</h1>
      <p className="text-gray-500 dark:text-gray-400 mt-2">内容可能已删除，或链接有误。</p>
      <Link href="/" className="btn-primary inline-block mt-6">返回首页</Link>
    </div>
  );
}
