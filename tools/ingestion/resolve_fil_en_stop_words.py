import openpyxl, re

wb = openpyxl.load_workbook('db/qw_en.xlsx', data_only=True)
sheet = wb['Fil']

stop_words = {'and', 'so', 'then', 'but', 'or', 'they', 'we', 'who', 'that', 'it', 'is', 'are', 'he', 'she', 'you', 'i', 'the', 'a', 'an', 'to', 'of', 'in', 'on', 'at', 'by', 'for', 'with', 'from'}

resolved = 0
unresolved = []

for r in range(2, sheet.max_row + 1):
    t_en = str(sheet.cell(row=r, column=13).value or '').strip()
    if t_en.lower() in stop_words:
        lemma = str(sheet.cell(row=r, column=2).value or '')
        meaning = str(sheet.cell(row=r, column=9).value or '')
        v_en = re.sub(r'\(\[([^\]]+)\]\)', r'\1', str(sheet.cell(row=r, column=14).value or ''))
        cit = str(sheet.cell(row=r, column=10).value or '')
        
        # Clean meaning words: e.g. "and do" -> "do", "We will forgive" -> "forgive", "they say" -> "say"
        # Find candidate keywords from meaning
        m_words = [w for w in re.findall(r'[a-zA-Z]+', meaning) if w.lower() not in stop_words]
        
        found = None
        for w in m_words:
            # Look for exact word or forms of w in v_en
            m = re.search(r'\b(' + re.escape(w) + r'(?:ed|ing|s|es|d)?)\b', v_en, re.IGNORECASE)
            if m:
                found = m.group(1)
                break
        
        if found:
            resolved += 1
        else:
            unresolved.append((r, sheet.cell(row=r, column=1).value, lemma, meaning, cit, v_en[:70]))

print(f'Resolved {resolved}/376 stop-word rows in Fil English.')
print(f'Unresolved: {len(unresolved)}')
for u in unresolved[:20]:
    print(' ', u)
