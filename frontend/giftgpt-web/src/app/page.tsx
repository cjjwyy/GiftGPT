import { Gift, Sparkles, Heart, ArrowRight, Search, Star } from 'lucide-react';
import Link from 'next/link';

export default function HomePage() {
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-primary-50 via-white to-rose-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 lg:py-32">
          <div className="max-w-3xl mx-auto text-center">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary-100 text-primary-700 text-sm font-medium mb-6">
              <Sparkles className="w-4 h-4" />
              AI 驱动 · 全链路闭环
            </div>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-gray-900 leading-tight mb-6">
              不猜，<span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-500 to-rose-500">更懂TA的心意</span>
            </h1>
            <p className="text-lg sm:text-xl text-gray-500 mb-10 max-w-xl mx-auto">
              消除"不知道送什么"的焦虑，让每一份礼物都恰到好处。<br />
              基于AI性格匹配 + 场景分析，一键获得专属礼物清单。
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
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
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_50%_120%,rgba(251,146,60,0.1),rgba(255,255,255,0))]" />
      </section>

      {/* Features */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-14">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">一站式送礼解决方案</h2>
            <p className="text-gray-500">从选品到送达，全链路智能化</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {[
              { icon: Search, title: '画像构建', desc: 'AI分析收礼人偏好，精准画像' },
              { icon: Sparkles, title: '智能匹配', desc: '性格×场景×预算，一键推荐' },
              { icon: Heart, title: '包装贺卡', desc: 'AI生成个性化祝福文案' },
              { icon: Gift, title: '物流追踪', desc: '一站式配送，收礼人双向互动' },
            ].map((f, i) => (
              <div key={i} className="text-center p-6 rounded-2xl hover:bg-gray-50 transition-colors">
                <div className="w-14 h-14 bg-primary-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
                  <f.icon className="w-7 h-7 text-primary-500" />
                </div>
                <h3 className="font-semibold text-gray-800 mb-2">{f.title}</h3>
                <p className="text-sm text-gray-500">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 bg-gradient-to-br from-primary-500 to-rose-500">
        <div className="max-w-3xl mx-auto px-4 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">准备好了吗？</h2>
          <p className="text-primary-50 mb-8 text-lg">现在开始，让AI为你挑选最合适的礼物</p>
          <Link href="/recommend" className="inline-flex items-center gap-2 bg-white text-primary-600 font-bold py-3 px-10 rounded-2xl text-lg hover:shadow-xl transition-all">
            免费开始使用 <ArrowRight className="w-5 h-5" />
          </Link>
        </div>
      </section>
    </div>
  );
}
