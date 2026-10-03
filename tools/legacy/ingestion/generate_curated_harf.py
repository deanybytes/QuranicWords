import os, sys, re, json, openpyxl

BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
DB_DIR = os.path.join(BASE_DIR, 'db')
CORPUS_DIR = os.path.join(DB_DIR, 'corpus_data')

# Load corpora
with open(os.path.join(CORPUS_DIR, 'quran_uthmani.json')) as f:
    uth_list = json.load(f)['verses']
uth_map = {v['verse_key']: v['text_uthmani'] for v in uth_list}

with open(os.path.join(CORPUS_DIR, 'chapters.json')) as f:
    chapters_data = json.load(f)['chapters']
surah_names = {c['id']: c['name_simple'] for c in chapters_data}

def load_trans(fname):
    with open(os.path.join(CORPUS_DIR, fname)) as f:
        data = json.load(f)
    items = data.get('translations', data.get('verses', []))
    res = {}
    for u, item in zip(uth_list, items):
        txt = re.sub(r'<sup[^>]*>.*?</sup>', '', item.get('text', ''))
        txt = re.sub(r'<[^>]+>', '', txt)
        txt = re.sub(r'\s+', ' ', txt).strip()
        res[u['verse_key']] = txt
    return res

trans_map = {
    'en': load_trans('quran_sahih.json'),
    'bn': load_trans('quran_bn_zakaria.json'),
    'bn_alt': load_trans('quran_bn_mujibur.json'),
    'ur': load_trans('quran_ur.json'),
    'hi': load_trans('quran_hi.json'),
    'id': load_trans('quran_id.json'),
    'ms': load_trans('quran_ms.json'),
    'tr': load_trans('quran_tr.json'),
    'fa': load_trans('quran_fa.json'),
    'ha': load_trans('quran_ha.json'),
    'sw': load_trans('quran_sw.json'),
}

def highlight_token(text, target):
    if not text or not target:
        return text, ''
    target_clean = target.strip()
    idx = text.find(target_clean)
    if idx >= 0:
        actual = text[idx:idx+len(target_clean)]
        return text[:idx] + f"([{actual}])" + text[idx+len(target_clean):], actual
    pattern = r'(?i)\b' + re.escape(target_clean) + r'\b'
    m = re.search(pattern, text)
    if m:
        actual = text[m.start():m.end()]
        return text[:m.start()] + f"([{actual}])" + text[m.end():], actual
    idx = text.lower().find(target_clean.lower())
    if idx >= 0:
        actual = text[idx:idx+len(target_clean)]
        return text[:idx] + f"([{actual}])" + text[idx+len(target_clean):], actual
    return text, target_clean

print("Setup ready.")
