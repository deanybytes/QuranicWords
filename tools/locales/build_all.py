#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Master builder for all 5 languages (hi, ms, fa, ha, sw) for QuranicWords."""

import os, re, json
import xml.etree.ElementTree as ET

import group1
import group2
import group3
import group4
import group5

BASE_XML = "app/src/main/res/values/strings.xml"

with open("tools/locales/base_en_map.json") as f:
    BASE_KEYS = json.load(f)

HI = {}
MS = {}
FA = {}
HA = {}
SW = {}

def set_trans(k, hi, ms, fa, ha, sw):
    if k not in BASE_KEYS:
        raise KeyError(f"Invalid key not in base XML: {k}")
    HI[k] = hi
    MS[k] = ms
    FA[k] = fa
    HA[k] = ha
    SW[k] = sw

# Register all 5 groups
group1.register(set_trans)
group2.register(set_trans)
group3.register(set_trans)
group4.register(set_trans)
group5.register(set_trans)

# Verify count
expected_count = len(BASE_KEYS)
for name, d in [('hi', HI), ('ms', MS), ('fa', FA), ('ha', HA), ('sw', SW)]:
    if len(d) != expected_count:
        missing = set(BASE_KEYS.keys()) - set(d.keys())
        extra = set(d.keys()) - set(BASE_KEYS.keys())
        raise ValueError(f"Language {name} key count mismatch: {len(d)} != {expected_count}. Missing: {missing}, Extra: {extra}")

print(f"All 5 languages verified: {expected_count} keys each.")

def escape_xml_text(s: str) -> str:
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = re.sub(r"(?<!\\)'", r"\'", s)
    s = re.sub(r'(?<!\\)"', r'\"', s)
    return s

def write_strings_xml(lang_tag: str, lang_dict: dict):
    out_dir = f"app/src/main/res/values-{lang_tag}"
    out_path = os.path.join(out_dir, "strings.xml")
    os.makedirs(out_dir, exist_ok=True)

    with open(BASE_XML, 'r', encoding='utf-8') as f:
        base_lines = f.readlines()

    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for line in base_lines:
        trimmed = line.strip()
        if trimmed.startswith('<!--') and trimmed.endswith('-->'):
            lines.append(f'    {trimmed}')
        elif trimmed.startswith('<string'):
            m = re.search(r'<string\s+name="([^"]+)"(.*?)>', trimmed)
            if m:
                k = m.group(1)
                extra_attrs = m.group(2)
                val = lang_dict[k]
                lines.append(f'    <string name="{k}"{extra_attrs}>{escape_xml_text(val)}</string>')
    lines.append('</resources>\n')

    with open(out_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))
    print(f"Written {out_path} ({len(lang_dict)} strings)")

# Write all 5 XML files
write_strings_xml('hi', HI)
write_strings_xml('ms', MS)
write_strings_xml('fa', FA)
write_strings_xml('ha', HA)
write_strings_xml('sw', SW)

print("All 5 language strings.xml generated successfully!")
