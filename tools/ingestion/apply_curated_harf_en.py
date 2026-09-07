import re
from tools.ingestion.build_harf_complete import HARF_CANONICAL, uth_map, trans_map

UPDATES = {
    (56, '[1]'): ('33:28', 'كُنتُنَّ', 'you (fem. pl.)', 'you'),
    (73, '[1]'): ('38:3', 'لَاتَ', 'not', 'not'),
    (105, '[1]'): ('2:77', 'أَوَلَا', 'do they not know', 'Do they not know'),
    (39, '[1]'): ('2:135', 'بَلْ', 'rather / nay', 'Rather'),
    (45, '[1]'): ('2:261', 'كَمَثَلِ', 'like / as', 'like'),
    (49, '[1]'): ('2:36', 'فَأَزَلَّهُمَا', 'then / so', 'But'),
    (53, '[1]'): ('9:40', 'هُمَا', 'they both', 'they'),
    (77, '[1]'): ('12:31', 'حَـٰشَ', 'God forbid / perfect is Allah', 'Perfect is Allāh'),
    (82, '[1]'): ('3:173', 'وَنِعْمَ', 'excellent / best', 'best'),
    (83, '[1]'): ('2:90', 'بِئْسَمَا', 'evil is that', 'How wretched is that'),
    (88, '[1]'): ('76:3', 'إِمَّا', 'either / whether', 'be he'),
    (92, '[1]'): ('7:132', 'مَهْمَا', 'whatever', 'No matter what'),
    (100, '[1]'): ('3:153', 'لِّكَيْلَا', 'so that not', 'so you would not'),
    (101, '[1]'): ('2:150', 'لِئَلَّا', 'in order that not', 'in order that'),
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
    (163, '[1]'): ('54:34', 'بِسَحَرٍ', 'before dawn', 'before dawn'),
    (167, '[1]'): ('4:71', 'جَمِيعًا', 'all together', 'all together'),
    (171, '[1]'): ('20:58', 'سُوًى', 'open / assigned', 'assigned'),
    (172, '[1]'): ('2:137', 'بِمِثْلِ', 'the same as', 'the same as'),
}

for k, v in UPDATES.items():
    HARF_CANONICAL[k] = v

fails = 0
for key, val in HARF_CANONICAL.items():
    vk, tar_ar, en_sense, en_tar = val
    v_ar = uth_map[vk]
    v_en = trans_map['en'][vk]
    
    if tar_ar not in v_ar:
        print(f'FAIL AR: {key} vk={vk} tar_ar={tar_ar}')
        fails += 1
    m = re.search(r'(?i)\b' + re.escape(en_tar) + r'\b', v_en)
    if not m and en_tar.lower() not in v_en.lower():
        print(f'FAIL EN: {key} vk={vk} en_tar={en_tar}')
        fails += 1

print(f'Audit finished. Total fails: {fails}/192')

# Fix remaining 3 prefixes
HARF_CANONICAL[(78, '[1]')] = ('3:146', 'كَأَيِّن', 'how many', 'how many')
HARF_CANONICAL[(82, '[1]')] = ('3:173', 'نِعْمَ', 'excellent / best', 'best')
HARF_CANONICAL[(138, '[1]')] = ('28:32', 'ذَٰنِكَ', 'those two', 'those')
