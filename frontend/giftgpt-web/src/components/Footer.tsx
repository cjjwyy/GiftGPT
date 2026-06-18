import Link from 'next/link';
import { Gift } from 'lucide-react';

export function Footer() {
  return (
    <footer className="bg-white border-t border-gray-100 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center gap-2 font-bold text-lg text-primary-600 mb-3">
              <Gift className="w-5 h-5" />
              <span>GiftGPT</span>
            </div>
            <p className="text-sm text-gray-500">AI 驱动的全链路礼物推荐平台<br />不猜，更懂TA的心意</p>
          </div>
          <div>
            <h4 className="font-semibold text-gray-800 mb-3">产品</h4>
            <div className="space-y-2 text-sm text-gray-500">
              <div>智能礼物推荐</div>
              <div>礼物社区故事</div>
              <div>企业团购服务</div>
            </div>
          </div>
          <div>
            <h4 className="font-semibold text-gray-800 mb-3">关于</h4>
            <div className="space-y-2 text-sm text-gray-500">
              <div>关于我们</div>
              <div>隐私政策</div>
              <div>用户协议</div>
            </div>
          </div>
          <div>
            <h4 className="font-semibold text-gray-800 mb-3">联系</h4>
            <div className="space-y-2 text-sm text-gray-500">
              <div>📧 contact@giftgpt.cn</div>
              <div>📱 微信公众号：GiftGPT</div>
            </div>
          </div>
        </div>
        <div className="mt-8 pt-6 border-t border-gray-100 text-center text-sm text-gray-400">
          © {new Date().getFullYear()} GiftGPT. All rights reserved. MIT License.
        </div>
      </div>
    </footer>
  );
}
