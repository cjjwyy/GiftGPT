"""Reproducible multi-annotator graph. Inputs are data, never executable instructions.

Only the latest revision within each supplied file wins. Across annotators, positive
classes are unioned, with one vote per source post/edge. Valuable disagreements are
quarantined, not silently resolved by timestamp. Inference is separately discounted.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import defaultdict
from pathlib import Path
from publish_annotations import atomic_write_json

ROOT = Path(__file__).resolve().parents[2]
RESOURCE = ROOT / 'backend/giftgpt-recommendation/src/main/resources/kg_keywords.json'
VOCAB = ROOT / 'frontend/giftgpt-web/src/lib/graphVocabulary.json'
REPORT = ROOT / 'crawl/reports/annotation-graph-audit.json'

# Canonical label | description | historical aliases. Interests and personality
# remain distinct even when annotators placed them in the wrong column.
PERSONALITY = [
 ('开朗','偏好轻松互动、表达与分享','热情|热血'),
 ('文艺','偏好文字、设计与富有审美的表达',''),
 ('浪漫','看重情感表达、纪念意义与仪式感','仪式感'),
 ('理性','看重明确用途、功能与使用体验',''),
 ('简约','偏好简洁造型与低负担设计',''),
 ('复古','偏好经典造型与怀旧元素',''),
 ('幽默','喜欢轻松有趣、创意搞怪的表达','沙雕|抽象'),
 ('可爱','偏好萌系形象与柔软亲切的设计',''),
 ('细致','重视耐心制作与细节体验','耐心'),
 ('条理','偏好整理、规划与秩序',''),
 ('自律','重视持续练习与习惯管理',''),
 ('感性','重视个人情绪与情感共鸣',''),
 ('安全感','偏好陪伴感与舒适氛围，不代表心理诊断','胆小'),
]
INTEREST = [
 ('科技','关注数码设备与技术体验','极客'),('养生','关注日常舒适与健康生活，不推断疗效','养生派|健康'),
 ('摄影','喜欢拍摄、影像记录与摄影器材','拍照'),('户外','喜欢户外活动与随行装备',''),
 ('音乐','喜欢聆听、演奏与音乐体验','贝斯'),('运动','喜欢体育活动与运动装备','篮球'),
 ('美食','喜欢食品、烹饪与风味体验','零食'),('咖啡','喜欢咖啡品鉴与冲煮',''),
 ('旅行','喜欢出游与旅途记录','旅游'),('阅读','喜欢书籍与阅读体验','爱读书|文学'),
 ('动漫','喜欢动漫角色与相关周边',''),('游戏','喜欢电子游戏与游戏周边',''),
 ('宠物','关注宠物与宠物主题设计',''),('时尚','关注服饰、配饰与穿搭','穿搭|精致'),
 ('美妆护肤','关注护肤、彩妆与香氛','爱美|护肤'),('艺术','喜欢绘画、艺术作品与创作',''),
 ('品茶','喜欢茶饮与茶器','茶'),('手工','喜欢拼装、手作与DIY',''),
 ('收藏','喜欢系列物品与纪念周边',''),('手账','喜欢书写、记录与手账制作','记录'),
 ('办公','关注办公便利与桌面舒适','工作'),('毛绒玩具','喜欢毛绒玩具与玩偶',''),
 ('居家','关注家居用品与日常生活体验',''),('园艺','喜欢植物与种植养护',''),
 ('酒饮','喜欢酒饮风味，仅适合明确成年的饮酒者','drink'),
]
RELATIONS = [
 ('恋人','伴侣关系，重视彼此偏好与情感表达','男朋友|女朋友|对象'),
 ('夫妻','婚姻伴侣，兼顾纪念意义与共同生活','丈夫|妻子|老公|老婆'),
 ('朋友','朋友间的心意与共同兴趣','闺蜜|好友|兄弟'),
 ('长辈','父母及其他长辈，关注实际需要与使用便利','父母|父亲|母亲|爸爸|妈妈'),
 ('晚辈','晚辈，需结合具体年龄和兴趣',''),('子女','自己的孩子，需结合年龄和实际需求','儿子|女儿|孩子'),
 ('兄弟姐妹','手足关系，结合熟悉的兴趣','哥哥|姐姐|弟弟|妹妹'),
 ('亲戚','其他亲属，优先合宜与具体需求',''),('家人','未细分的家庭关系，不自动推断辈分',''),
 ('同事','职场往来，关注实用、适度与边界',''),('同学','同学往来与共同经历',''),
 ('师生','师生间表达心意，应遵守相关赠礼规范','老师|教师'),
 ('医生','感谢医护，应遵守机构规定，避免贵重馈赠','医护'),('其他','尚未归类的关系',''),
]
OCCASIONS = [
 ('birthday','生日','庆祝个人成长与心意',''),('anniversary','纪念日','纪念共同经历与情感',''),
 ('valentines','情人节','表达伴侣间的爱意',''),('festival','节庆','节日问候与分享','节庆送礼'),
 ('graduation','毕业','纪念校园经历与新阶段',''),('proposal','求婚','表达承诺与共同未来',''),
 ('mothers_day','母亲节','向母亲表达关怀',''),('fathers_day','父亲节','向父亲表达关怀',''),
 ('teachers_day','教师节','适度表达对教师的感谢',''),('christmas','圣诞节','节日祝福与惊喜','圣诞'),
 ('thank_you','感谢','表达谢意，注意关系边界',''),('daily','日常','没有特定节日的日常心意','无特定场景|日常搞怪'),
 ('visit_patient','探病','探望慰问，以收礼人实际需要为准','看望病人'),
 ('family_visit','提亲','家庭见面与礼节表达',''),('other','其他','未明确或其他场景','不限'),
]
EMPTY = {'与兴趣无关','与性格无关','不限','未知','无','抽烟'}
KEYWORD_ALIASES = {'蓝牙音响':'蓝牙音箱','复古音响':'复古音箱','书':'图书','包包':'包','茶':'茶叶',
                   '美妆镜':'化妆镜','diy小屋':'DIY小屋','旅游':'旅行体验','旅行':'旅行体验',
                   'ipad':'iPad','iphone':'iPhone','macbook':'MacBook','ps5':'PS5','switch':'Switch'}
# Unambiguous product cues, not guessed recipient personality. Style inference is
# restricted to explicit words; broad personality-to-product propagation is unsafe.
RULES = {
 '阅读':'书签|书立|读书|阅读器|图书|哈利波特|一千零一夜',
 '音乐':'音箱|音响|耳机|唱片|留声机|琴包|吉他|尤克里里|电子琴|演唱会|黑胶',
 '摄影':'相机|拍立得|摄影|云台|自拍杆', '运动':'筋膜枪|球衣|球鞋|篮球|运动鞋|运动相机',
 '科技':'键盘|鼠标|数据线|充电宝|无人机|机器人|智能手|手机|电脑|ipad|iphone|macbook',
 '游戏':'ps5|switch|游戏机|游戏手柄|电竞|塞尔达|守望先锋|魔兽|炉石|星际争霸',
 '动漫':'手办|动漫|fufu|痛包|谷子|吧唧|色纸|立牌|伍六七|cos服',
 '美妆护肤':'口红|唇膏|唇蜜|护肤|护手霜|身体乳|面膜|洁面|眼霜|眼影|彩妆|化妆|香水|小棕瓶|神仙水|美容仪',
 '时尚':'项链|手链|耳环|耳钉|手镯|戒指|丝巾|皮带|领带|钱包|手表|挎包|托特包|毛衣链',
 '美食':'巧克力|蛋糕|零食|糖|糕点|水果|车厘子|薯片|芒果干|马卡龙|蜂蜜|牛奶|奶粉|月饼',
 '咖啡':'咖啡|滤杯|手冲|磨豆机|粉锤', '品茶':'茶叶|红茶|茶具|茶壶|紫砂壶|铁观音|茶水',
 '养生':'按摩|足浴|泡脚|颈枕|腰垫|艾草锤|蒸汽眼罩|保暖|养生壶',
 '户外':'防晒冰袖|防水手机袋|折叠椅|户外', '旅行':'拉杆箱|行李箱|旅行体验',
 '手账':'手账|笔记本|钢笔|日历', '手工':'diy|积木|拼图|串珠|手作|手工书',
 '艺术':'油画|挂画|木雕|钉子画|纸雕|弦丝画', '宠物':'宠物',
 '办公':'鼠标垫|电脑增高架|桌面|手机支架|座椅靠垫', '毛绒玩具':'毛绒|玩偶|公仔|棉花娃娃',
 '居家':'净水器|抱枕|杯|毛毯|餐具|碗|锅|盘子|电视|空气净化|睡衣|工具箱|垃圾袋|焖烧|吹风机|梳子|投影仪|脚踏凳|鸵鸟枕|木盒',
 '园艺':'盆栽|多肉', '酒饮':'白酒|葡萄酒|香槟|果酒|气泡酒',
 '浪漫':'情书|情侣|告白|永生花|玫瑰花|同心锁', '复古':'复古|留声机|黑胶',
 '文艺':'文创|手账|书签|纸雕|手写|诗集', '条理':'收纳|备忘录',
}
RULES['美食'] += '|雪蜜|燕窝|海参|银耳|黑芝麻糊'
RULES['时尚'] += '|机械表|对表|小包|手串|转运珠|小饰品'
RULES['养生'] += '|护颈|睡眠仪|血压计|体检卡|智能药盒'
RULES['美妆护肤'] += '|染发剂|剃须刀'
RULES['品茶'] += '|养生茶'
RULES['手工'] += '|针织花束|自制奖牌'

def vocabulary():
    result = {}
    for key, rows in [('personality', PERSONALITY), ('interest', INTEREST), ('relation', RELATIONS)]:
        result[key] = [dict(value=n, label=n, description=d, aliases=a.split('|') if a else []) for n,d,a in rows]
    result['occasion'] = [dict(value=c,label=n,description=d,aliases=a.split('|') if a else []) for c,n,d,a in OCCASIONS]
    return result

VOC = vocabulary()
ALIASES = {}
for kind, rows in VOC.items():
    group = 'tag' if kind in ('personality','interest') else kind
    ALIASES.setdefault(group, {})
    for row in rows:
        for alias in [row['value'], row['label']] + row['aliases']:
            ALIASES[group][alias] = row['label']

def canonical(kind, value):
    value = ' '.join(str(value or '').split())
    if not value or (kind != 'occasion' and value in EMPTY):
        return ''
    return ALIASES.get(kind, {}).get(value, value)

def infer(keyword):
    return [tag for tag, cues in RULES.items() if any(cue in keyword.lower() for cue in cues.split('|'))]

def build(inputs):
    versions = defaultdict(list)
    manifest, issues = [], []
    for label, annotation_path, corpus_path in inputs:
        corpus = {}
        for line in Path(corpus_path).read_text(encoding='utf-8-sig').splitlines():
            if line.strip():
                record = json.loads(line)
                corpus[record['id']] = record
        raw = Path(annotation_path).read_bytes()
        manifest.append(dict(label=label, file=str(annotation_path), sha256=hashlib.sha256(raw).hexdigest()))
        latest = {}
        for line in raw.decode('utf-8-sig').splitlines():
            if line.strip():
                record = json.loads(line)
                if record['id'] not in latest or record.get('ts',0) >= latest[record['id']].get('ts',0):
                    latest[record['id']] = record
        for post_id, record in latest.items():
            if post_id not in corpus:
                issues.append(dict(id=post_id, issue='missing_source', file=str(annotation_path)))
                continue
            versions[post_id].append((record, corpus[post_id], len(manifest)-1))
    evidence = defaultdict(lambda: {'manual':set(), 'inferred':set()})
    keyword_posts = defaultdict(set)
    source_index = {}
    conflicts = []
    for post_id, entries in sorted(versions.items()):
        source_index[post_id] = dict(url=entries[0][1].get('url',''), inputs=[e[2] for e in entries])
        if len({r.get('valuable') for r,_,_ in entries}) > 1:
            conflicts.append(dict(id=post_id, issue='valuable_disagreement', versions=[r for r,_,_ in entries]))
            continue
        signatures = {json.dumps(r.get('classes',[]),sort_keys=True,ensure_ascii=False) for r,_,_ in entries}
        if len(signatures)>1:
            conflicts.append(dict(id=post_id, issue='class_disagreement_union', versions=[r for r,_,_ in entries]))
        for record, source, _ in entries:
            if record.get('valuable') is not True:
                continue
            for cl in record.get('classes',[]):
                nodes = {k:[canonical(k,cl.get(k))] for k in ('gender','relation','occasion')}
                nodes['tag'] = [canonical('tag',t) for t in cl.get('interest_tags',[])+cl.get('personality_tags',[])]
                for original in set(cl.get('keywords',[])):
                    keyword = KEYWORD_ALIASES.get(original.strip().lower(),original.strip())
                    if not keyword or keyword.lower() in {'夏天','move','free益节氨糖软骨素','alexander','wang','studio','display','greta','jelly','苹果apple'}:
                        issues.append(dict(id=post_id,issue='ambiguous_fragment',keyword=original))
                        continue
                    keyword_posts[keyword].add(post_id)
                    for kind, labels in nodes.items():
                        for label in set(labels)-{''}:
                            evidence[(kind,label,keyword)]['manual'].add(post_id)
                    for tag in infer(keyword):
                        evidence[('tag',tag,keyword)]['inferred'].add(post_id)
    # Per-node normalization avoids broad labels dominating simply by corpus size.
    support = {edge:len(e['manual']) + .35*len(e['inferred']-e['manual']) for edge,e in evidence.items()}
    maxima = defaultdict(float)
    for (kind,node,kw), n in support.items():
        maxima[kind,node] = max(maxima[kind,node],n)
    graph = {k:{} for k in ('gender','relation','occasion','tag')}
    edges=[]
    for edge, e in sorted(evidence.items()):
        kind,node,keyword=edge
        manual=sorted(e['manual']); inferred=sorted(e['inferred']-e['manual'])
        # A pure inferred link can never reach the weight of a confirmed one.
        confidence = 1 if manual else .35
        shrinkage = support[edge] / (support[edge] + 2)
        weight=max(1,round(100*confidence*shrinkage*math.log1p(support[edge])/math.log1p(maxima[kind,node])))
        graph[kind].setdefault(node,{})[keyword]=weight
        edges.append(dict(type=kind,node=node,keyword=keyword,weight=weight,manualPosts=manual,inferredPosts=inferred))
    manual_tags={e['keyword'] for e in edges if e['type']=='tag' and e['manualPosts']}
    all_tags={e['keyword'] for e in edges if e['type']=='tag'}
    stats=dict(uniquePosts=len(versions),keywords=len(keyword_posts),edges=len(edges),
               manuallyTaggedKeywords=len(manual_tags),taggedKeywords=len(all_tags),
               inferredOnlyEdges=sum(not e['manualPosts'] for e in edges),conflicts=len(conflicts))
    graph['_meta']=dict(version=2,aliases=ALIASES,vocabulary=VOC,edges=edges,stats=stats)
    report=dict(stats=stats,inputs=manifest,sources=source_index,conflicts=conflicts,issues=issues,
                untaggedKeywords=sorted(set(keyword_posts)-all_tags),
                weighting='one post per edge; s=manual+0.35*inferred; weight=100*confidence*s/(s+2)*log1p(s)/log1p(nodeMax); inferred-only confidence=0.35')
    return graph,report

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input',nargs=3,action='append',required=True,metavar=('LABEL','ANNOTATIONS','SUMMARIES'))
    args=p.parse_args()
    graph,report=build(args.input)
    if not report['stats']['edges']:
        raise ValueError('Empty graph: refusing to replace runtime data')
    atomic_write_json(RESOURCE,graph)
    atomic_write_json(VOCAB,VOC)
    atomic_write_json(REPORT,report)
    print(json.dumps(report['stats'],ensure_ascii=False))

if __name__=='__main__':
    main()
