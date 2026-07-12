import { Gift, Sparkles, Heart, ArrowRight, Search } from 'lucide-react';
import Link from 'next/link';

export default function HomePage() {
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-24 lg:pt-28 lg:pb-32">
          <div className="max-w-3xl mx-auto text-center">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/70 dark:bg-gray-900/60 backdrop-blur border border-primary-100/70 dark:border-primary-900/40 text-primary-700 dark:text-primary-300 text-sm font-medium mb-7 shadow-soft">
              <Sparkles className="w-3.5 h-3.5" />
              AI 驱动 · 全链路闭环
            </div>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-gray-900 dark:text-white leading-[1.1] mb-6 tracking-tight">
              不猜，<span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-500 to-rose-500">更懂TA的心意</span>
            </h1>
            <p className="text-lg sm:text-xl text-gray-500 dark:text-gray-400 mb-10 max-w-xl mx-auto leading-relaxed">
              消除&ldquo;不知道送什么&rdquo;的焦虑，让每一份礼物都恰到好处。
              <br className="hidden sm:block" />
              基于AI性格匹配 + 场景分析，一键获得专属礼物清单。
            </p>
            <div className="flex flex-col sm:flex-row gap-3.5 justify-center">
              <Link href="/recommend" className="btn-primary text-lg py-3 px-8 inline-flex items-center gap-2">
                <Gift className="w-5 h-5" />
                开始选礼物
                <ArrowRight className="w-5 h-5" />
              </Link>
              <Link href="/stories" className="btn-outline text-lg py-3 px-8">
                看送礼故事
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-14">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-3">一站式送礼解决方案</h2>
            <p className="text-gray-500 dark:text-gray-400">从选品到送达，全链路智能化</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { icon: Search, title: '画像构建', desc: 'AI分析收礼人偏好，精准画像' },
              { icon: Sparkles, title: '智能匹配', desc: '性格×场景×预算，一键推荐' },
              { icon: Heart, title: '包装贺卡', desc: 'AI生成个性化祝福文案' },
              { icon: Gift, title: '物流追踪', desc: '一站式配送，收礼人双向互动' },
            ].map((f, i) => (
              <div key={i} className="card text-center hover:shadow-lift hover:-translate-y-0.5">
                <div className="w-14 h-14 bg-gradient-to-br from-primary-50 to-rose-50 dark:from-primary-900/30 dark:to-rose-900/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
                  <f.icon className="w-6 h-6 text-primary-500 dark:text-primary-400" />
                </div>
                <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-1.5">{f.title}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="pb-24">
        <div className="max-w-5xl mx-auto px-4">
          <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary-500 to-rose-500 px-8 py-16 text-center shadow-lift">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(255,255,255,0.18),transparent_50%)]" />
            <div className="relative">
              <h2 className="text-3xl font-bold text-white mb-3">准备好了吗？</h2>
              <p className="text-primary-50 mb-8 text-lg">现在开始，让AI为你挑选最合适的礼物</p>
              <Link href="/recommend" className="inline-flex items-center gap-2 bg-white text-primary-600 font-semibold py-3 px-10 rounded-xl text-lg hover:shadow-xl hover:-translate-y-0.5 transition-all">
                免费开始使用 <ArrowRight className="w-5 h-5" />
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
