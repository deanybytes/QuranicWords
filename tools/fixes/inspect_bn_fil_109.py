import json, re

data = json.load(open('bn_fil_pairs.json'))
print(f"Total Fil unique pairs: {len(data)}")

STOP_WORDS = {'যে', 'যা', 'তা', 'এই', 'ঐ', 'সে', 'তিনি', 'তারা', 'আমরা', 'তোমরা', 'আপনি', 'আমি', 'তুমি', 'কী', 'কে', 'না', 'নি', 'বা', 'এবং', 'কিন্তু', 'অথবা', 'হতে', 'থেকে', 'দ্বারা', 'দিয়ে', 'জন্য', 'প্রতি', 'উপর'}
PUNCT = set(' \t\n\r()[]{}.,;:!?/\\\"\'‘’-—।؛؟<>«»')

suspicious = []
for k, v in data.items():
    cit, tar_ar = k.split(' ||| ')
    c13 = str(v.get('c13') or '').strip()
    c9 = str(v.get('c9') or '').strip()
    v_text = str(v.get('v') or '').strip()
    
    # Check stop word
    if c13 in STOP_WORDS:
        suspicious.append((k, 'STOP_WORD', c13, v_text[:60]))
        continue
        
    # Check chop
    for m in re.finditer(r'\(\[([^\]]+)\]\)', v_text):
        start, end = m.start(), m.end()
        char_before = v_text[start - 1] if start > 0 else ' '
        char_after = v_text[end] if end < len(v_text) else ' '
        if char_before not in PUNCT or char_after not in PUNCT:
            suspicious.append((k, 'CHOP', f'{char_before}[{m.group(1)}]{char_after}', v_text[max(0, start-10):min(len(v_text), end+10)]))

print(f"Suspicious Fil pairs: {len(suspicious)}")
for s in suspicious:
    print(s)
