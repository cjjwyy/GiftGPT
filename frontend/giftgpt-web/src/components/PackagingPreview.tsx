'use client';

import { useRef } from 'react';

interface Props {
  theme: string; ribbonColor: string; ribbonStyle: string; ribbonText: string;
  cardText: string; customs: Set<string>; productName: string;
}

export default function PackagingPreview(p: Props) {
  const ref = useRef<SVGSVGElement>(null);
  const colors: Record<string, string> = { classic: '#653f54', korean: '#f0e8dd', kraft: '#c79a64', luxury: '#203e44', acrylic: '#e2eff0' };
  const ribbon = ({ '金色': '#d6b368', '红色': '#ba4658', '粉色': '#e8a8be', '蓝色': '#688dad', '白色': '#faf7f0' } as Record<string, string>)[p.ribbonColor] || '#d6b368';
  const download = () => {
    if (!ref.current) return;
    const text = new XMLSerializer().serializeToString(ref.current);
    const url = URL.createObjectURL(new Blob([text], { type: 'image/svg+xml;charset=utf-8' }));
    const a = document.createElement('a'); a.href = url; a.download = 'gift-packaging.svg'; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return <section className="card mb-6">
    <div className="flex justify-between gap-3 items-center"><h2 className="font-semibold">包装实时预览</h2><button type="button" className="btn-outline text-sm" disabled={!p.theme} onClick={download}>导出预览 SVG</button></div>
    <p className="text-xs text-gray-500 mt-2">效果示意，非实物尺寸；自定义颜色未识别时使用金色。照片夹仅作占位展示。</p>
    <svg ref={ref} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 440" role="img" aria-label="包装方案预览" className="w-full max-w-xl mx-auto" style={{ fontFamily: 'sans-serif' }}>
      <title>礼物包装方案</title>
      <rect width="640" height="440" rx="20" fill="#fcf8f4" />
      <text x="320" y="35" textAnchor="middle" fill="#57453e" fontSize="17">{p.productName.slice(0, 25) || '你的心意，即将启程'}</text>
      <rect x="116" y="78" width="380" height="258" rx="15" fill={colors[p.theme] || '#dedede'} stroke="#aa9278" strokeWidth="2" />
      {p.theme === 'acrylic' && <rect x="133" y="95" width="346" height="224" rx="10" fill="#ffffff" fillOpacity="0.55" stroke="#bacfd1" />}
      <g transform={p.ribbonStyle === 'side' ? 'rotate(-14 306 207)' : undefined}>
        <rect x="285" y="78" width="42" height="258" fill={ribbon} />
        {p.ribbonStyle !== 'side' && <rect x="116" y="164" width="380" height="30" fill={ribbon} />}
        <path d="M306 172 C230 108 231 216 306 172 C381 108 381 216 306 172" fill={ribbon} stroke="#9b793e" strokeWidth="3" />
        {p.ribbonStyle === 'double_bow' && <path d="M306 155 C246 89 256 168 306 155 C366 89 356 168 306 155" fill={ribbon} stroke="#9b793e" strokeWidth="2" />}
        {p.ribbonStyle === 'furoshiki' && <path d="M132 95 L306 177 L480 95 M132 320 L306 177 L480 320" fill="none" stroke={ribbon} strokeWidth="22" />}
      </g>
      {p.customs.has('band_wrap') && <rect x="116" y="268" width="380" height="32" fill="#f4e6d0" />}
      {p.customs.has('ribbon_text') && <text x="310" y="187" textAnchor="middle" fill="#4b3020" fontSize="13">{p.ribbonText.slice(0, 10)}</text>}
      {p.customs.has('dried_flower') && <g stroke="#748564" strokeWidth="3"><path d="M160 267 L199 212 M160 267 L147 215" /><circle cx="199" cy="212" r="13" fill="#dba8ad" /><circle cx="147" cy="215" r="10" fill="#e2c88e" /></g>}
      {p.customs.has('polaroid') && <g transform="rotate(9 468 260)"><rect x="420" y="206" width="90" height="112" fill="white" stroke="#aaa" /><rect x="430" y="217" width="70" height="69" fill="#e9ddd2" /><text x="465" y="256" textAnchor="middle" fontSize="12" fill="#765">照片位</text></g>}
      {p.customs.has('greeting_card') && <g><rect x="172" y="307" width="290" height="100" rx="5" fill="#fffefa" stroke="#cbb997" /><text x="190" y="334" fill="#745c4c" fontSize="13">心意贺卡</text>{[0, 1, 2].map(i => <text key={i} x="190" y={356 + i * 18} fill="#51443e" fontSize="12">{p.cardText.slice(i * 19, (i + 1) * 19)}</text>)}</g>}
      {p.customs.has('scent') && <text x="485" y="372" fill="#806956" fontSize="13">香氛装饰</text>}
    </svg>
  </section>;
}
