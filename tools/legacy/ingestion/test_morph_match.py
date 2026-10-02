import re

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

def strip_tashkeel(text):
    t = str(text).replace('\u0670', 'ا').replace('\u06E5', 'و').replace('\u06E6', 'ي').replace('\u0640', '')
    t = re.sub(r'[\u064B-\u065F\u06D6-\u06ED\uFEFF]', '', t)
    t = re.sub(r'[إأآٱ]', 'ا', t)
    t = t.replace('ة', 'ه').replace('ى', 'ي')
    return t.strip()

print('qaAla ->', bw_to_ar('qaAla'), 'strip ->', strip_tashkeel(bw_to_ar('qaAla')))
print('قَالَ strip ->', strip_tashkeel('قَالَ'))
assert strip_tashkeel(bw_to_ar('qaAla')) == strip_tashkeel('قَالَ')
print('Match SUCCESS!')
