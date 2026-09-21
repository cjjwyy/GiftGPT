import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import build_competition_graph as builder


class CompetitionGraphTest(unittest.TestCase):
    def test_aliases_and_explicit_keyword_inference(self):
        self.assertEqual('美妆护肤', builder.canonical('tag', '爱美'))
        self.assertEqual('日常', builder.canonical('occasion', '无特定场景'))
        self.assertEqual('师生', builder.canonical('relation', '老师'))
        self.assertEqual('', builder.canonical('tag', '与兴趣无关'))
        self.assertIn('音乐', builder.infer('复古蓝牙音箱'))
        self.assertNotIn('开朗', builder.infer('复古蓝牙音箱'))

    def test_multiple_annotators_count_once_and_conflicts_quarantined(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            cl = dict(relation='朋友', occasion='生日', gender='不限',
                      interest_tags=[], personality_tags=['文艺'], keywords=['手账本','手账本'])
            records = [dict(id='p1',valuable=True,classes=[cl,cl]),
                       dict(id='p2',valuable=True,classes=[cl]),
                       dict(id='p3',valuable=True,classes=[dict(cl, keywords=['未知礼物'])])]
            source = root / 'summaries.jsonl'
            source.write_text('\n'.join(json.dumps(dict(id=r['id'])) for r in records),encoding='utf8')
            paths=[]
            for i in range(2):
                path=root / ('annotations%d.jsonl'%i)
                copy=[dict(r) for r in records]
                if i: copy[1]['valuable']=False
                path.write_text('\n'.join(json.dumps(r) for r in copy),encoding='utf8')
                paths.append(('label1',path,source))
            graph,audit=builder.build(paths)
            edge=next(e for e in graph['_meta']['edges'] if e['type']=='tag' and e['node']=='文艺' and e['keyword']=='手账本')
            self.assertEqual(['p1'],edge['manualPosts'])
            self.assertEqual(1, len(audit['conflicts']))
            inferred=next(e for e in graph['_meta']['edges'] if e['node']=='手账')
            self.assertLessEqual(inferred['weight'],35)
            self.assertEqual(['p1'],inferred['inferredPosts'])

    def test_frontend_vocabulary_is_generated_from_same_source(self):
        self.assertEqual(builder.VOC,json.loads(builder.VOCAB.read_text(encoding='utf8')))

    def test_published_edges_have_provenance_and_bounded_weights(self):
        graph=json.loads(builder.RESOURCE.read_text(encoding='utf8'))
        for e in graph['_meta']['edges']:
            self.assertTrue(e['manualPosts'] or e['inferredPosts'])
            self.assertEqual(len(e['manualPosts']),len(set(e['manualPosts'])))
            self.assertFalse(set(e['manualPosts']) & set(e['inferredPosts']))
            self.assertTrue(0 < e['weight'] <= 100)
            if not e['manualPosts']: self.assertLessEqual(e['weight'],35)
            self.assertEqual(e['weight'],graph[e['type']][e['node']][e['keyword']])


if __name__=='__main__':
    unittest.main()
