'use client';

import { useRef, useState } from 'react';
import Image from 'next/image';
import { toast } from 'react-hot-toast';
import { GIFT_BOXES, CUSTOMIZATIONS, RIBBON_STYLES } from '@/lib/packagingCatalog';

interface Props {
  theme: string; ribbonColor: string; ribbonStyle: string; ribbonText: string;
  cardText: string; scent: string; customs: Set<string>; productName: string;
}

const DISCLAIMER = 'AI 生成款式参考，非组合实拍；图片中的文字、颜色与配件不代表最终定制效果。';

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new window.Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error('图片加载失败，请稍后重试'));
    image.src = src;
  });
}

function wrapText(ctx: CanvasRenderingContext2D, value: string, width: number) {
  const lines: string[] = [];
  for (const paragraph of value.split('\n')) {
    let line = '';
    for (const char of paragraph) {
      if (line && ctx.measureText(line + char).width > width) {
        lines.push(line);
        line = '';
      }
      line += char;
    }
    lines.push(line);
  }
  return lines;
}

export default function PackagingPreview(p: Props) {
  const [exporting, setExporting] = useState(false);
  const exportBusy = useRef(false);
  const box = GIFT_BOXES.find(item => item.id === p.theme);
  const ribbon = RIBBON_STYLES.find(item => item.id === p.ribbonStyle);
  const extras = CUSTOMIZATIONS.filter(item => p.customs.has(item.id));
  const notes = [
    p.customs.has('ribbon_text') && `礼带文字：${p.ribbonText || '待填写'} · 烫字颜色：${p.ribbonColor || '未选择'}`,
    p.customs.has('greeting_card') && `贺卡内容：${p.cardText || '待填写'}`,
    p.customs.has('scent') && `香型：${p.scent || '未选择'}`,
  ].filter((note): note is string => Boolean(note));

  const download = async () => {
    if (!box || exportBusy.current) return;
    exportBusy.current = true;
    setExporting(true);
    try {
      // Compose generated raster assets with actual selections, never SVG.
      const items = [box, ...(ribbon ? [ribbon] : []), ...extras];
      const images = await Promise.all(items.map(item => loadImage(item.image)));
      const imageMap = new Map(items.map((item, i) => [item.image, images[i]]));
      await document.fonts.ready;
      const canvas = document.createElement('canvas');
      canvas.width = 1200;
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('当前浏览器不支持图片导出');
      ctx.font = '24px sans-serif';
      const titleLines = wrapText(ctx, p.productName || '你的心意，即将启程', 1104);
      const noteLines = notes.flatMap(note => wrapText(ctx, note, 1104));
      const top = 126 + titleLines.length * 32;
      const footer = top + 584 + (extras.length ? 244 : 0);
      canvas.height = footer + noteLines.length * 34 + 104;
      ctx.fillStyle = '#fcf8f2';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = '#473c37';
      ctx.font = 'bold 36px sans-serif';
      ctx.fillText('GiftGPT · 包装搭配方案', 48, 66);
      ctx.font = '24px sans-serif';
      titleLines.forEach((line, i) => ctx.fillText(line, 48, 110 + i * 32));
      ctx.drawImage(imageMap.get(box.image)!, 48, top, 540, 540);
      ctx.font = 'bold 30px sans-serif';
      ctx.fillText(box.name, 640, top + 42);
      ctx.font = '22px sans-serif';
      wrapText(ctx, box.desc, 510).forEach((line, i) => ctx.fillText(line, 640, top + 84 + i * 30));
      if (ribbon) {
        ctx.drawImage(imageMap.get(ribbon.image)!, 640, top + 144, 280, 280);
        ctx.fillText(`绑法参考 · ${ribbon.name}`, 640, top + 464);
      } else {
        ctx.fillText('绑法：未选择', 640, top + 180);
      }
      extras.forEach((item, i) => {
        const x = 48 + i * 184;
        ctx.drawImage(imageMap.get(item.image)!, x, top + 584, 168, 168);
        ctx.font = '20px sans-serif';
        ctx.fillText(item.name, x, top + 786);
      });
      ctx.font = '24px sans-serif';
      noteLines.forEach((line, i) => ctx.fillText(line, 48, footer + i * 34));
      ctx.fillStyle = '#786b61';
      ctx.font = '20px sans-serif';
      ctx.fillText(DISCLAIMER, 48, canvas.height - 40);
      const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob(
        value => value ? resolve(value) : reject(new Error('图片导出失败，请重试')), 'image/png',
      ));
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'gift-packaging.png';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '图片导出失败，请重试');
    } finally {
      exportBusy.current = false;
      setExporting(false);
    }
  };

  return <section className="card mb-6" aria-label="包装搭配方案">
    <div className="flex flex-wrap justify-between gap-3 items-center">
      <h2 className="font-semibold">包装搭配方案</h2>
      <button type="button" className="btn-outline text-sm disabled:opacity-40" disabled={!box || exporting} onClick={download}>
        {exporting ? '正在导出…' : '导出方案 PNG'}
      </button>
    </div>
    <p className="text-xs text-gray-500 dark:text-gray-400 mt-2 leading-relaxed">{DISCLAIMER}</p>
    {!box ? <div className="mt-4 rounded-2xl bg-stone-50 dark:bg-gray-800 p-10 text-center text-sm text-gray-500 dark:text-gray-400">
      选择一款礼盒，开始搭配你的心意。
    </div> : <div className="mt-5 grid gap-5 md:grid-cols-2">
      <div>
        <Image src={box.image} alt={`${box.name}款式参考`} width={640} height={640} unoptimized className="w-full rounded-2xl aspect-square object-cover" />
        <p className="mt-3 text-sm text-gray-600 dark:text-gray-300 break-words">{p.productName || '你的心意，即将启程'}</p>
      </div>
      <div className="min-w-0">
        <h3 className="text-xl font-semibold">{box.name}</h3>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-2">{box.desc}</p>
        {ribbon ? <div className="mt-4 flex items-center gap-4">
          <Image src={ribbon.image} alt={`${ribbon.name}绑法参考`} width={112} height={112} unoptimized className="w-24 h-24 rounded-xl object-cover" />
          <div><p className="text-xs text-gray-500 dark:text-gray-400">绑法参考</p><p className="font-medium mt-1">{ribbon.name}</p><p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{ribbon.desc}</p></div>
        </div> : <p className="text-sm text-gray-500 dark:text-gray-400 mt-4">绑法尚未选择</p>}
        {extras.length > 0 && <div className="grid grid-cols-3 gap-3 mt-5">
          {extras.map(item => <div key={item.id}>
            <Image src={item.image} alt={item.name} width={128} height={128} unoptimized className="w-full rounded-xl aspect-square object-cover" />
            <p className="mt-1.5 text-xs text-gray-600 dark:text-gray-300">{item.name}</p>
          </div>)}
        </div>}
        {notes.length > 0 && <div className="mt-4 space-y-2 border-t border-stone-200 dark:border-gray-700 pt-4">
          {notes.map((note, i) => <p key={i} className="text-sm text-gray-700 dark:text-gray-200 break-words whitespace-pre-wrap">{note}</p>)}
        </div>}
      </div>
    </div>}
  </section>;
}
