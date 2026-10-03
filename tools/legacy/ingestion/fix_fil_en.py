import openpyxl, re

wb = openpyxl.load_workbook('db/qw_en.xlsx')
ws = wb['Fil']

# Curation rules for Fil sheet
# Key is (citation, target_ar) or citation
FIXES = {
    ('Al-Baqarah 2:10', 'كَانُوا۟'): {
        'target_en': 'used to',
        'sub': (r'because (\[they\]) \[habitually\] used to lie', r'because they [habitually] ([used to]) lie')
    },
    ('Al-Baqarah 2:8', 'ءَامَنَّا'): {
        'target_en': 'believe',
        'sub': (r'"\(\[We\]\) believe in', r'"We ([believe]) in')
    },
    ('Al-Baqarah 2:82', 'عَمِلُوا۟'): {
        'target_en': 'do',
        'sub': (r'who believe \(\[and\]\) do righteous deeds', r'who believe and ([do]) righteous deeds')
    },
    ('Al-Furqan 25:42', 'صَبَرْنَا'): {
        'target_ar': 'يَرَوْنَ',
        'target_en': 'they see',
        'v_ar': 'إِن كَادَ لَيُضِلُّنَا عَنْ ءَالِهَتِنَا لَوْلَآ أَن صَبَرْنَا عَلَيْهَا ۚ وَسَوْفَ يَعْلَمُونَ حِينَ ([يَرَوْنَ]) ٱلْعَذَابَ مَنْ أَضَلُّ سَبِيلًا',
        'sub': (r'had \(\[we\]\) not been steadfast in \[worship of\] them\." But they are going to know, when they see the punishment,',
                r'had we not been steadfast in [worship of] them." But they are going to know, when ([they see]) the punishment,')
    },
    ('Al-Baqarah 2:255', 'شَآءَ'): {
        'target_en': 'He wills',
        'sub': (r'except for what \(\[He\]\) wills', r'except for what ([He wills])')
    },
    ('Al-Baqarah 2:10', 'يَكْذِبُونَ'): {
        'target_en': 'lie',
        'sub': (r'because \(\[they\]\) \[habitually\] used to lie', r'because they [habitually] used to ([lie])')
    },
    ('Al-Baqarah 2:186', 'دَعَانِ'): {
        'target_en': 'calls upon Me',
        'sub': (r'when \(\[he\]\) calls upon Me', r'when he ([calls upon Me])')
    },
    ('Al-Hijr 15:18', 'أَتْبَعَهُۥ'): {
        'target_en': 'is pursued',
        'sub': (r'steals a hearing \(\[and\]\) is pursued by', r'steals a hearing and ([is pursued]) by')
    },
    ('An-Nahl 16:113', 'أَخَذَهُمُ'): {
        'target_en': 'seized them',
        'sub': (r'denied him; \(\[so\]\) punishment seized them', r'denied him; so punishment ([seized them])')
    },
    ('Ali \'Imran 3:117', 'ظَلَمُوٓا۟'): {
        'target_en': 'have wronged',
        'sub': (r'of a people \(\[who\]\) have wronged themselves', r'of a people who ([have wronged]) themselves')
    },
    ('Al-Baqarah 2:35', 'كُلَا'): {
        'target_en': 'eat',
        'sub': (r'dwell, \(\[you\]\) and your wife, in Paradise and eat therefrom', r'dwell, you and your wife, in Paradise and ([eat]) therefrom')
    },
    ('Al-A\'raf 7:173', 'أَشْرَكَ'): {
        'target_en': 'associated',
        'sub': (r'that \(\[our\]\) fathers associated', r'that our fathers ([associated])')
    },
    ('Al-Baqarah 2:75', 'يَسْمَعُونَ'): {
        'target_en': 'hear',
        'sub': (r'a party of them \(\[used\]\) to hear', r'a party of them used to ([hear])')
    },
    ('Al-Baqarah 2:10', 'زَادَهُمُ'): {
        'target_en': 'has increased',
        'sub': (r'disease, \(\[so\]\) Allāh has increased their disease', r'disease, so Allāh ([has increased]) their disease')
    },
    ('Al-Baqarah 2:58', 'نَّغْفِرْ'): {
        'target_en': 'forgive',
        'sub': (r'and We will \(\[forgive\]\) you', r'and We will ([forgive]) you')
    },
    ('Ibrahim 14:13', 'أَوْحَىٰٓ'): {
        'target_en': 'inspired',
        'sub': (r'\(\[So\]\) their Lord inspired to them', r'So their Lord ([inspired]) to them')
    },
    ('Al-Baqarah 2:30', 'نُسَبِّحُ'): {
        'target_en': 'exalt You with praise',
        'sub': (r'while \(\[we\]\) exalt You with praise', r'while we ([exalt You with praise])')
    },
    ('Ali \'Imran 3:122', 'لْيَتَوَكَّلِ'): {
        'target_en': 'should rely',
        'sub': (r'the believers \(\[should\]\) rely', r'the believers ([should rely])')
    },
    ('Al-Baqarah 2:155', 'بَشِّرِ'): {
        'target_en': 'give good tidings',
        'sub': (r'fruits, \(\[but\]\) give good tidings to the patient', r'fruits, but ([give good tidings]) to the patient')
    },
    ('Ali \'Imran 3:117', 'أَهْلَكَتْهُ'): {
        'target_en': 'destroys it',
        'sub': (r'sinned\] and \(\[destroys\]\) it', r'sinned] and ([destroys it])')
    },
    ('Al-Baqarah 2:176', 'ٱخْتَلَفُوا۟'): {
        'target_en': 'differ',
        'sub': (r'those \(\[who\]\) differ over', r'those who ([differ]) over')
    },
    ('Al-Baqarah 2:218', 'جَـٰهَدُوا۟'): {
        'target_en': 'fought',
        'sub': (r'emigrated \(\[and\]\) fought in the cause', r'emigrated and ([fought]) in the cause')
    },
    ('Al-An\'am 6:6', 'أَنشَأْنَا'): {
        'target_en': 'brought forth',
        'sub': (r'their sins and \(\[brought\]\) forth after them', r'their sins and ([brought forth]) after them')
    },
    ('Ali \'Imran 3:144', 'ٱنقَلَبْتُمْ'): {
        'target_en': 'turn back',
        'sub': (r'would you \(\[turn\]\) back on your heels', r'would you ([turn back]) on your heels')
    },
    ('Al-Baqarah 2:219', 'تَتَفَكَّرُونَ'): {
        'target_en': 'give thought',
        'sub': (r'that you might \(\[give\]\) thought', r'that you might ([give thought])')
    },
    ('An-Nisa 4:3', 'تَعْدِلُوا۟'): {
        'target_en': 'be just',
        'sub': (r'that you will not \(\[be\]\) just', r'that you will not ([be just])')
    },
    ('Al-Baqarah 2:222', 'يَطْهُرْنَ'): {
        'target_en': 'are pure',
        'sub': (r'until \(\[they\]\) are pure', r'until they ([are pure])')
    },
    ('Al-Baqarah 2:30', 'نُقَدِّسُ'): {
        'target_en': 'declare Your perfection',
        'sub': (r'and \(\[declare\]\) Your perfection', r'and ([declare Your perfection])')
    },
    ('Ali \'Imran 3:49', 'أَنفُخُ'): {
        'target_en': 'breathe',
        'sub': (r'then I \(\[breathe\]\) into it', r'then I ([breathe]) into it')
    },
    ('Yunus 10:109', 'يَحْكُمَ'): {
        'target_en': 'will judge',
        'sub': (r'until \(\[Allāh\]\) will judge', r'until Allāh ([will judge])')
    },
    ('Al-Baqarah 2:175', 'أَصْبَرَهُمْ'): {
        'target_en': 'patient they are',
        'sub': (r'How \(\[patient\]\) they are for', r'How ([patient they are]) for')
    },
    ('Al-Baqarah 2:48', 'يُنصَرُونَ'): {
        'target_en': 'be aided',
        'sub': (r'nor \(\[will\]\) they be aided', r'nor will they ([be aided])')
    },
    ('Al-Baqarah 2:76', 'تَحَ'): {
        'target_ar': 'فَتَحَ',
        'v_ar': 'وَإِذَا لَقُوا۟ ٱلَّذِينَ ءَامَنُوا۟ قَالُوٓا۟ ءَامَنَّا وَإِذَا خَلَا بَعْضُهُمْ إِلَىٰ بَعْضٍ قَالُوٓا۟ أَتُحَدِّثُونَهُم بِمَا ([فَتَحَ]) ٱللَّهُ عَلَيْكُمْ لِيُحَآجُّوكُم بِهِۦ عِندَ رَبِّكُمْ ۚ أَفَلَا تَعْقِلُونَ',
        'target_en': 'has revealed',
        'sub': (r'what Allāh \(\[has\]\) revealed to you', r'what Allāh ([has revealed]) to you')
    },
    ('Ali \'Imran 3:132', 'تُرْحَمُونَ'): {
        'target_en': 'obtain mercy',
        'sub': (r'that you may \(\[obtain\]\) mercy', r'that you may ([obtain mercy])')
    },
    ('Ali \'Imran 3:188', 'يُحْمَدُوا۟'): {
        'target_en': 'to be praised',
        'sub': (r'and like \(\[to\]\) be praised for', r'and like ([to be praised]) for')
    },
    ('Ali \'Imran 3:188', 'يَفْعَلُوا۟'): {
        'target_en': 'do',
        'sub': (r'what they did not \(\[do\]\)', r'what they did not ([do])')
    },
    ('Al-Baqarah 2:200', 'قَضَيْتُم'): {
        'target_en': 'have completed',
        'sub': (r'when you \(\[have\]\) completed your rites', r'when you ([have completed]) your rites')
    },
    ('Al-An\'am 6:79', 'طَرَ'): {
        'target_ar': 'فَطَرَ',
        'v_ar': 'إِنِّى وَجَّهْتُ وَجْهِىَ لِلَّذِى ([فَطَرَ]) ٱلسَّمَـٰوَٰتِ وَٱلْأَرْضَ حَنِيفًا ۖ وَمَآ أَنَا۠ مِنَ ٱلْمُشْرِكِينَ',
        'target_en': 'created',
        'sub': (r'Him who \(\[created\]\) the heavens', r'Him who ([created]) the heavens')
    },
    ('Al-Isra 17:34', 'بِٱلَّتِى'): {
        'target_ar': 'يَبْلُغَ',
        'v_ar': 'وَلَا تَقْرَبُوا۟ مَالَ ٱلْيَتِيمِ إِلَّا بِٱلَّتِى هِىَ أَحْسَنُ حَتَّىٰ ([يَبْلُغَ]) أَشُدَّهُۥ ۚ وَأَوْفُوا۟ بِٱلْعَهْدِ ۖ إِنَّ ٱلْعَهْدَ كَانَ مَسْـُٔولًا',
        'target_en': 'reaches',
        'sub': (r'until he \(\[reaches\]\) maturity', r'until he ([reaches]) maturity')
    },
    ('Al-Baqarah 2:26', 'أَرَادَ'): {
        'target_en': 'intend',
        'sub': (r'What \(\[did\]\) Allāh intend by this', r'What did Allāh ([intend]) by this')
    },
    ('Ali \'Imran 3:193', 'مُنَادِيًا'): {
        'target_ar': 'يُنَادِى',
        'v_ar': 'رَّبَّنَآ إِنَّنَا سَمِعْنَا مُنَادِيًا ([يُنَادِى]) لِلْإِيمَـٰنِ أَنْ ءَامِنُوا۟ بِرَبِّكُمْ فَـَٔامَنَّا',
        'target_en': 'calling',
        'sub': (r'heard a caller \(\[calling\]\) to faith', r'heard a caller ([calling]) to faith')
    },
    ('An-Nur 24:5', 'أَصْلَحُوا۟'): {
        'target_en': 'reform',
        'sub': (r'repent thereafter \(\[and\]\) reform', r'repent thereafter and ([reform])')
    }
}

updated_count = 0
for r in range(2, ws.max_row + 1):
    cit = str(ws.cell(r, 10).value or '').strip()
    t_ar = str(ws.cell(r, 11).value or '').strip()
    
    rule = FIXES.get((cit, t_ar))
    if not rule:
        continue
        
    if 'target_ar' in rule:
        ws.cell(r, 11).value = rule['target_ar']
    if 'v_ar' in rule:
        ws.cell(r, 12).value = rule['v_ar']
    if 'target_en' in rule:
        ws.cell(r, 13).value = rule['target_en']
        
    v_en = str(ws.cell(r, 14).value or '')
    pat, rep = rule['sub']
    new_v_en = re.sub(pat, rep, v_en)
    if new_v_en == v_en:
        # If bracket pattern didn't match directly, try a more flexible match
        clean_v = re.sub(r'\(\[([^\]]+)\]\)', r'\1', v_en)
        # re-bracket the target_en in clean_v
        tar = rule.get('target_en', ws.cell(r, 13).value)
        m = re.search(r'\b(' + re.escape(tar) + r')\b', clean_v, re.IGNORECASE)
        if m:
            s, e = m.span()
            new_v_en = clean_v[:s] + f'([{clean_v[s:e]}])' + clean_v[e:]
            
    ws.cell(r, 14).value = new_v_en
    updated_count += 1

print(f'Successfully updated {updated_count} rows in Fil sheet.')
wb.save('db/qw_en.xlsx')
print('Saved db/qw_en.xlsx.')
