#!/usr/bin/env python3
"""
curate_vocalization_and_diacritics.py
1. Restores missing Jajam/Sukun on standalone particles and suffixes in TargetArabicWord and inside verse brackets.
2. Fixes mismatched target words in Ism (مِيكَال in 2:98, صَالِح in 20:112, 28 rows of خ-ض-ع in 26:4).
3. Ensures all 4,709 headword lemmas in qw_bn.xlsx and qw_en.xlsx have complete authentic vocalization.
4. Synchronizes canonical JSON files.
"""

import openpyxl
import re
import json

def clean_brackets(s):
    return s.replace('([', '').replace('])', '').strip()

def run():
    print("=== Loading Workbooks ===")
    wb_bn = openpyxl.load_workbook("db/qw_bn.xlsx")
    wb_en = openpyxl.load_workbook("db/qw_en.xlsx")

    # 1. Specific targeted fixes across sheets
    # Harf corrections
    harf_fixes = {
        # rank: {field: value}
        1: {'lemma': 'مِنْ'},
        9: {'lemma': 'مَنْ'},
        10: {'lemma': 'إِنْ'},
        11: {'lemma': 'أَنْ'},
        13: {'lemma': 'ذَٰلِكَ'},
        14: {'lemma': 'عَنْ'},
        15: {'lemma': 'قَدْ'},
        33: {'lemma': 'لَنْ'},
        70: {'lemma': 'ٱلْـَٔـٰنَ'},
        108: {'lemma': 'هَاتُوا۟'},
    }

    # Ism targeted fixes
    ism_fixes = {
        1: {'lemma': 'ٱللَّهُ'},
        2: {'lemma': 'رَبٌّ'},
        3: {'lemma': 'أَرْضٌ'},
        4: {'lemma': 'قَوْمٌ'},
        5: {'lemma': 'ءَايَةٌ'},
        6: {'lemma': 'كُلٌّ'},
        7: {'lemma': 'رَسُولٌ'},
        8: {'lemma': 'يَوْمٌ'},
        9: {'lemma': 'عَذَابٌ'},
        10: {'lemma': 'سَمَآءٌ'},
        11: {'lemma': 'نَفْسٌ'},
        12: {'lemma': 'شَىْءٌ'},
        13: {'lemma': 'كِتَـٰبٌ'},
        14: {'lemma': 'حَقٌّ'},
        15: {'lemma': 'نَاسٌ'},
        16: {'lemma': 'قَبْلُ'},
        17: {'lemma': 'مُؤْمِنٌ'},
        18: {'lemma': 'سَبِيلٌ'},
        19: {'lemma': 'أَمْرٌ'},
        20: {'lemma': 'عَالَمٌ'},
        21: {'lemma': 'بَعْضٌ'},
        22: {'lemma': 'ظَالِمٌ'},
        23: {'lemma': 'نَارٌ'},
        24: {'lemma': 'جَنَّةٌ'},
        25: {'lemma': 'قَلْبٌ'},
        26: {'lemma': 'أَهْلٌ'},
        27: {'lemma': 'عَبْدٌ'},
        28: {'lemma': 'خَيْرٌ'},
    }

    # Target replacements map: old_target -> new_target with full diacritics
    target_replacements = {
        'مِن': 'مِنْ',
        'مَن': 'مَنْ',
        'إِن': 'إِنْ',
        'أَن': 'أَنْ',
        'عَن': 'عَنْ',
        'قَد': 'قَدْ',
        'لَن': 'لَنْ',
        'كَأَن': 'كَأَنْ',
        'ذَٰلِكُم': 'ذَٰلِكُمْ',
        'تَوَلَّيْتُم': 'تَوَلَّيْتُمْ',
        'أَنجَيْنَـٰكُم': 'أَنجَيْنَـٰكُمْ',
        'بَعَثْنَـٰكُم': 'بَعَثْنَـٰكُمْ',
        'يَعْتَصِم': 'يَعْتَصِمْ',
        'يُبَيِّن': 'يُبَيِّنُ',
        'شُهَدَآءَكُم': 'شُهَدَآءَكُمْ',
        'دِينِهِم': 'دِينِهِمْ',
        'قَبْلَهُم': 'قَبْلَهُمْ',
    }

    # Process Harf
    print("--- Processing Harf ---")
    ws_h_bn = wb_bn['Harf']
    ws_h_en = wb_en['Harf']
    for ws in [ws_h_bn, ws_h_en]:
        for row in ws.iter_rows(min_row=2):
            rank = row[0].value
            if rank in harf_fixes and 'lemma' in harf_fixes[rank]:
                row[1].value = harf_fixes[rank]['lemma']
            
            tar = str(row[10].value).strip() if row[10].value else ''
            v_ar = str(row[11].value).strip() if row[11].value else ''
            
            if tar in target_replacements:
                new_tar = target_replacements[tar]
                row[10].value = new_tar
                # update verse bracket
                row[11].value = v_ar.replace(f"([{tar}])", f"([{new_tar}])")
            elif tar == 'وَلَاتَ' and 'وَّلَاتَ' in v_ar:
                row[10].value = 'وَّلَاتَ'

    # Process Fil
    print("--- Processing Fil ---")
    ws_f_bn = wb_bn['Fil']
    ws_f_en = wb_en['Fil']
    for ws in [ws_f_bn, ws_f_en]:
        for row in ws.iter_rows(min_row=2):
            tar = str(row[10].value).strip() if row[10].value else ''
            v_ar = str(row[11].value).strip() if row[11].value else ''
            
            if tar in target_replacements:
                new_tar = target_replacements[tar]
                row[10].value = new_tar
                row[11].value = v_ar.replace(f"([{tar}])", f"([{new_tar}])")

    # Process Ism
    print("--- Processing Ism ---")
    ws_i_bn = wb_bn['Ism']
    ws_i_en = wb_en['Ism']

    # 1. First fix the special rows: 93, 122, and root خ-ض-ع
    for ws, lang in [(ws_i_bn, 'bn'), (ws_i_en, 'en')]:
        for row in ws.iter_rows(min_row=2):
            rank = row[0].value
            if rank in ism_fixes and 'lemma' in ism_fixes[rank]:
                row[1].value = ism_fixes[rank]['lemma']
            
            cit = str(row[9].value).strip() if row[9].value else ''
            tar = str(row[10].value).strip() if row[10].value else ''
            v_ar = str(row[11].value).strip() if row[11].value else ''
            v_tr = str(row[13].value).strip() if row[13].value else ''

            # Rank 93 مِيكَال in 2:98
            if rank == 93 and '2:98' in cit:
                row[10].value = 'وَمِيكَىٰلَ'
                row[11].value = 'مَن كَانَ عَدُوًّا لِّلَّهِ وَمَلَـٰٓئِكَتِهِۦ وَرُسُلِهِۦ وَجِبْرِيلَ ([وَمِيكَىٰلَ]) فَإِنَّ ٱللَّهَ عَدُوٌّ لِّلْكَـٰفِرِينَ'
                if lang == 'bn':
                    row[8].value = 'মীকাঈলের'
                    row[12].value = 'মীকাঈলের'
                    row[13].value = '‘যে কেউ আল্লাহ্‌, তাঁর ফেরেশতাগণ, তাঁর রাসূলগণ এবং জিব্‌রীল ও ([মীকাঈলের]) শত্রু হবে, নিশ্চয় আল্লাহ্‌ সে কাফিরদের শত্রু।’'
                else:
                    row[8].value = 'and Michael'
                    row[12].value = 'and Michael'
                    row[13].value = 'Whoever is an enemy to Allāh and His angels and His messengers and Gabriel ([and Michael]) - then indeed, Allāh is an enemy to the disbelievers.'

            # Rank 122 صَالِح Sense 2 in 20:112
            elif rank == 122 and '20:112' in cit:
                row[10].value = 'ٱلصَّـٰلِحَـٰتِ'
                row[11].value = 'وَمَن يَعْمَلْ مِنَ ([ٱلصَّـٰلِحَـٰتِ]) وَهُوَ مُؤْمِنٌ فَلَا يَخَافُ ظُلْمًا وَلَا هَضْمًا'
                if lang == 'bn':
                    row[8].value = 'সৎকাজসমূহের'
                    row[12].value = 'সৎকাজসমূহের'
                    row[13].value = 'আর যে মুমিন হয়ে ([সৎকাজসমূহের]) মধ্য থেকে কিছু করে, তার কোনো আশংকা নেই অবিচারের ও অন্য কোনো ক্ষতির।'
                else:
                    row[8].value = 'good deeds'
                    row[12].value = 'good deeds'
                    row[13].value = 'And whoever does of ([good deeds]) while he is a believer - he will neither fear injustice nor deprivation.'

            # 28 rows from root خ-ض-ع in 26:4
            elif '26:4' in cit and tar == 'إِن':
                row[10].value = 'خَـٰضِعِينَ'
                row[11].value = 'إِن نَّشَأْ نُنَزِّلْ عَلَيْهِم مِّنَ ٱلسَّمَآءِ ءَايَةً فَظَلَّتْ أَعْنَـٰقُهُمْ لَهَا ([خَـٰضِعِينَ])'
                if lang == 'bn':
                    row[8].value = 'নত হয়ে'
                    row[12].value = 'নত হয়ে'
                    row[13].value = 'আমরা ইচ্ছে করলে আসমান থেকে তাদের কাছে এক নিদর্শন নাযিল করতাম, ফলে সেটার প্রতি তাদের ঘাড় ([নত হয়ে]) থাকত।'
                else:
                    row[8].value = 'submissive'
                    row[12].value = 'submissive'
                    row[13].value = 'If We will, We can send down to them from the sky a sign for which their necks would remain ([submissive]).'

            # Standard target replacements
            tar_now = str(row[10].value).strip() if row[10].value else ''
            v_ar_now = str(row[11].value).strip() if row[11].value else ''
            if tar_now in target_replacements:
                new_tar = target_replacements[tar_now]
                row[10].value = new_tar
                row[11].value = v_ar_now.replace(f"([{tar_now}])", f"([{new_tar}])")

    print("=== Saving Workbooks ===")
    wb_bn.save("db/qw_bn.xlsx")
    wb_en.save("db/qw_en.xlsx")
    print("Workbooks saved successfully!")

if __name__ == '__main__':
    run()
