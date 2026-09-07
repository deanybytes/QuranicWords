import json, re

with open('db/corpus_data/quran_uthmani.json') as f:
    uth_map = {v['verse_key']: v['text_uthmani'] for v in json.load(f)['verses']}

def load_t(fname):
    data = json.load(open(f'db/corpus_data/{fname}'))
    items = data.get('translations', data.get('verses', []))
    res = {}
    for u, item in zip(json.load(open('db/corpus_data/quran_uthmani.json'))['verses'], items):
        txt = re.sub(r'<sup[^>]*>.*?</sup>', '', item.get('text', ''))
        txt = re.sub(r'<[^>]+>', '', txt)
        res[u['verse_key']] = re.sub(r'\s+', ' ', txt).strip()
    return res

sahih = load_t('quran_sahih.json')
bn = load_t('quran_bn_zakaria.json')
bn_alt = load_t('quran_bn_mujibur.json')
ur = load_t('quran_ur.json')

test_cases = [
    # (rank, s_idx, vk, tar_ar, en_tar, bn_tar, ur_tar)
    (1, '[1]', '2:22', 'مِنَ', 'from', 'হতে', 'سے'),
    (1, '[2]', '2:25', 'مِن', 'of', 'হতে', 'سے'),
    (2, '[1]', '2:176', 'فِى', 'in', 'মধ্যে', 'میں'),
    (2, '[2]', '2:65', 'فِى', 'concerning', 'সম্পর্কে', 'میں'),
    (3, '[1]', '2:6', 'إِنَّ', 'Indeed', 'নিশ্চয়ই', 'بے شک'),
    (3, '[2]', '2:143', 'إِنَّ', 'Indeed', 'নিশ্চয়ই', 'یقیناً'),
    (4, '[1]', '2:5', 'عَلَىٰ', 'upon', 'উপর', 'پر'),
    (4, '[2]', '2:89', 'عَلَى', 'against', 'বিরুদ্ধে', 'مقابلے'),
    (5, '[1]', '2:17', 'ٱلَّذِى', 'who', 'যে', 'جو'),
    (5, '[2]', '2:25', 'ٱلَّذِى', 'which', 'যা', 'جو'),
]

for tc in test_cases:
    rk, s_idx, vk, tar_ar, en_tar, bn_tar, ur_tar = tc
    ar_ok = tar_ar in uth_map[vk]
    en_ok = re.search(r'(?i)\b' + re.escape(en_tar) + r'\b', sahih[vk]) is not None
    bn_v = bn[vk] if bn_tar in bn[vk] else bn_alt[vk]
    bn_ok = bn_tar in bn_v
    ur_ok = ur_tar in ur[vk]
    print(f'#{rk} {s_idx} vk={vk}: AR={ar_ok}, EN={en_ok}, BN={bn_ok}, UR={ur_ok}')
    if not (ar_ok and en_ok and bn_ok and ur_ok):
        print('  FAIL:', tc)
