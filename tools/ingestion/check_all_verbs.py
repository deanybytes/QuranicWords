import re
from collections import Counter

BW_MAP = {
    "'": 'ء', '>': 'أ', '<': 'إ', '|': 'آ', '{': 'ٱ', '&': 'ؤ', '}': 'ئ',
    'A': 'ا', 'b': 'ب', 'p': 'ة', 't': 'ت', 'v': 'ث', 'j': 'ج', 'H': 'ح',
    'x': 'خ', 'd': 'د', '*': 'ذ', 'r': 'ر', 'z': 'ز', 's': 'س', '$': 'ش',
    'S': 'ص', 'D': 'ض', 'T': 'ط', 'Z': 'ظ', 'E': 'ع', 'g': 'غ', '_': 'ـ',
    'f': 'ف', 'q': 'ق', 'k': 'ك', 'l': 'ل', 'm': 'م', 'n': 'ن', 'h': 'ه',
    'w': 'و', 'Y': 'ى', 'y': 'ي', 'F': 'ً', 'N': 'ٌ', 'K': 'ٍ', 'a': 'َ',
    'u': 'ُ', 'i': 'ِ', '~': 'ّ', 'o': 'ْ', '`': 'ٰ', '^': 'ٓ'
}

def bw_to_ar(bw):
    return ''.join(BW_MAP.get(c, c) for c in bw)

verb_lemmas = Counter()
with open('db/corpus_data/quranic-corpus-morphology-0.4.txt') as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith('#') or line.startswith('LOCATION'):
            continue
        parts = line.split('\t')
        if len(parts) < 4: continue
        feat = parts[3]
        if 'POS:V' in feat:
            m = re.search(r'LEM:([^\|]+)', feat)
            if m:
                lem = bw_to_ar(m.group(1))
                verb_lemmas[lem] += 1

print(f'Total occurrences of verbs: {sum(verb_lemmas.values())}')
print(f'Total unique verb lemmas in Quran: {len(verb_lemmas)}')
print('Top 20 verbs:')
for lem, count in verb_lemmas.most_common(20):
    print(f'  {lem}: {count}')
