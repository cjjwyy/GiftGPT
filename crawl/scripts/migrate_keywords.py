"""迁移旧标注(post_annotations.jsonl)到新「节点→关键词」格式，并聚合 kg_keywords.json。

旧 class: category/opt_names/supplements
新 class: gender/relation/interest_tags/personality_tags/keywords
"""
import json, os, sys
from collections import Counter

def load_jsonl(path):
    out=[]
    if os.path.exists(path):
        for line in open(path, encoding='utf-8'):
            line=line.strip()
            if not line: continue
            try: out.append(json.loads(line))
            except Exception: continue
    return out

def split(text):
    return [x.strip() for x in (text or '').replace('、','，').replace('；','，').split('，') if x.strip()]

def migrate_class(cls):
    kws=list(cls.get('keywords') or [])
    for o in cls.get('opt_names') or []:
        for x in split(o):
            if x and x not in kws: kws.append(x)
    for dk in ('interest_tag_supplements','personality_tag_supplements'):
        for vals in (cls.get(dk) or {}).values():
            for x in (vals or []):
                if x and x not in kws: kws.append(x)
    # 补充项内容
    for dk in ('interest_supplements','personality_supplements'):
        for vals in (cls.get(dk) or {}).values():
            for x in (vals or []):
                if x and x not in kws: kws.append(x)
    return {
        'class_id': cls.get('class_id') or '',
        'gender': cls.get('gender') or '',
        'relation': cls.get('relation') or '',
        'interest_tags': cls.get('interest_tags') or [],
        'personality_tags': cls.get('personality_tags') or [],
        'keywords': kws,
    }

def migrate_file(src, dst):
    recs=load_jsonl(src)
    out=[]
    for r in recs:
        r=dict(r)
        r['classes']=[migrate_class(c) for c in r.get('classes') or []]
        out.append(r)
    with open(dst,'w',encoding='utf-8') as f:
        for r in out:
            f.write(json.dumps(r, ensure_ascii=False)+'\n')
    print(f'migrated {len(out)} records -> {dst}')

def build_graph(recs):
    kw_gender=Counter(); kw_relation=Counter(); kw_tag=Counter()
    for r in recs:
        if not r.get('valuable'): continue
        for cls in r.get('classes') or []:
            kws=[k.strip() for k in (cls.get('keywords') or []) if k and k.strip()]
            if not kws: continue
            if cls.get('gender'):
                for kw in kws: kw_gender[(cls['gender'],kw)]+=1
            if cls.get('relation'):
                for kw in kws: kw_relation[(cls['relation'],kw)]+=1
            for t in cls.get('interest_tags') or []:
                for kw in kws: kw_tag[(t,kw)]+=1
            for t in cls.get('personality_tags') or []:
                for kw in kws: kw_tag[(t,kw)]+=1
    def nest(c):
        d={}
        for (node,kw),cnt in c.items(): d.setdefault(node,{})[kw]=cnt
        return d
    return {'gender':nest(kw_gender),'relation':nest(kw_relation),'tag':nest(kw_tag)}

def save_graph(recs, path):
    g=build_graph(recs)
    with open(path,'w',encoding='utf-8') as f:
        json.dump(g,f,ensure_ascii=False,indent=2)
    print('saved graph ->', path, {k:len(v) for k,v in g.items()})

if __name__=='__main__':
    # 桌面 alpha
    base1=r'C:\Users\cjForCodes\Desktop\label\label (alpha)'
    base2=r'C:\Users\cjForCodes\Desktop\label\label (alpha) 2'
    for base in (base1,base2):
        src=os.path.join(base,'data','post_annotations.jsonl')
        if os.path.exists(src):
            migrate_file(src, src)
            recs=load_jsonl(src)
            save_graph(recs, os.path.join(base,'data','kg_keywords.json'))
    # 项目 crawl 的桌面副本没有 post_annotations，仅输出说明
    print('done')
