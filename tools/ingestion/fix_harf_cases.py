import re
from tools.ingestion.build_harf_complete import HARF_CANONICAL, uth_map, trans_map

# Exact string corrections
UPDATES = {
    (56, '[1]'): ('33:28', 'كُنتُنَّ', 'you (fem. pl.)', 'you'),
    (73, '[1]'): ('38:3', 'لَاتَ', 'there was not / no longer', 'no longer'),
    (105, '[1]'): ('2:77', 'أَوَلَا', 'do they not know', 'Do they not know'),
    (39, '[1]'): ('2:135', 'بَلْ', 'rather / nay', 'Rather'),
    (45, '[1]'): ('2:261', 'كَمَثَلِ', 'like / as', 'like'),
    (49, '[1]'): ('2:36', 'فَأَزَلَّهُمَا', 'then / so', 'But'),
    (53, '[1]'): ('9:40', 'هُمَا', 'they both', 'they'),
    (77, '[1]'): ('12:31', 'حَـٰشَ', 'God forbid', 'Allāh forbid'),
    (82, '[1]'): ('3:173', 'وَنِعْمَ', 'excellent / best', 'best'),
    (83, '[1]'): ('2:90', 'بِئْسَمَا', 'evil is that', 'How wretched is that'),
    (88, '[1]'): ('76:3', 'إِمَّا', 'either / whether', 'be he'),
    (92, '[1]'): ('7:132', 'مَهْمَا', 'whatever', 'No matter what'),
    (100, '[1]'): ('3:153', 'لِّكَيْلَا', 'so that not', 'so that'),
    (101, '[1]'): ('2:150', 'لِئَلَّا', 'so that not', 'so that'),
    (102, '[1]'): ('3:64', 'أَلَّا', 'that we not', 'that we will not'),
    (107, '[1]'): ('6:150', 'هَلُمَّ', 'bring forward', 'Bring forward'),
    (111, '[1]'): ('17:23', 'أُفٍّ', 'fie / uff', 'uff'),
    (117, '[1]'): ('18:79', 'وَرَآءَهُم', 'after / behind them', 'after them'),
    (118, '[1]'): ('75:5', 'أَمَامَهُۥ', 'ahead / in sin', 'to continue in sin'),
    (119, '[1]'): ('2:255', 'خَلْفَهُمْ', 'after / behind them', 'after them'),
    (135, '[1]'): ('41:29', 'ٱلَّذَيْنِ', 'the two who', 'those who'),
    (141, '[1]'): ('28:35', 'أَنتُمَا', 'you two', 'you both'),
    (153, '[1]'): ('17:110', 'أَيًّا مَّا', 'whichever', 'Whichever'),
    (161, '[1]'): ('79:46', 'عَشِيَّةً', 'an afternoon / evening', 'afternoon'),
    (162, '[1]'): ('20:59', 'ضُحًى', 'mid-morning / daylight', 'mid-morning'),
    (163, '[1]'): ('54:34', 'بِسَحَرٍ', 'by dawn', 'by dawn'),
    (167, '[1]'): ('4:71', 'جَمِيعًا', 'as one / together', 'as one'),
    (171, '[1]'): ('20:58', 'سُوًى', 'open / equidistant', 'open'),
    (172, '[1]'): ('2:137', 'بِمِثْلِ', 'the same as', 'the same as'),
}

for k, v in UPDATES.items():
    HARF_CANONICAL[k] = v

ar_fails = []
en_fails = []

for key, val in HARF_CANONICAL.items():
    vk, tar_ar, en_sense, en_tar = val
    v_ar = uth_map.get(vk, '')
    v_en = trans_map['en'].get(vk, '')
    
    if tar_ar not in v_ar:
        ar_fails.append((key, vk, tar_ar, v_ar[:60]))
        
    m = re.search(r'(?i)\b' + re.escape(en_tar) + r'\b', v_en)
    if not m and en_tar.lower() not in v_en.lower():
        en_fails.append((key, vk, en_tar, v_en[:80]))

print(f'AR fails: {len(ar_fails)}')
for f in ar_fails:
    print('  AR fail:', f)
print(f'EN fails: {len(en_fails)}')
for f in en_fails:
    print('  EN fail:', f)
