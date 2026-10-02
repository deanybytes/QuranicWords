import openpyxl, json, re

# Load uthmani and translations
corpus_dir = 'db/corpus_data'
with open(f'{corpus_dir}/quran_uthmani.json') as f:
    uth_list = json.load(f)['verses']
uth_map = {v['verse_key']: v['text_uthmani'] for v in uth_list}

def load_trans(fname):
    with open(f'{corpus_dir}/{fname}') as f:
        data = json.load(f)
    items = data.get('translations', data.get('verses', []))
    res = {}
    for u, item in zip(uth_list, items):
        txt = re.sub(r'<sup[^>]*>.*?</sup>', '', item.get('text', ''))
        txt = re.sub(r'<[^>]+>', '', txt)
        txt = re.sub(r'\s+', ' ', txt).strip()
        res[u['verse_key']] = txt
    return res

trans = {
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

print("Corpus loaded.")
