"""Old-content -> new-content maps so released installs keep their progress. For each shipped
content version, every old lesson is mapped to the *new* word ids it taught; the app then marks
as completed the longest run of new lessons whose words the learner had already studied."""
import json
import subprocess
from collections import Counter, defaultdict

from . import arabic, config

# content version -> git ref that shipped it (ContentSeeder.CONTENT_VERSION)
RELEASES = {35: "v1.0.0", 34: "v1.0.1"}


def _show(ref, path):
    out = subprocess.run(["git", "-C", str(config.ROOT), "show", f"{ref}:{path}"], capture_output=True, check=True)
    return json.loads(out.stdout)


def _surface_index(qac, gtaf_en, shipped_keys):
    """Skeleton of every written form (whole token and bare stem) -> most frequent lemma key."""
    idx = defaultdict(Counter)
    for (s, a), ws in qac.items():
        toks = gtaf_en[(s, a)]
        for w, segs in ws.items():
            for g in segs:
                if g.prefix or g.suffix or not g.lemma:
                    continue
                key = arabic.nfc(f"{g.coarse}|{g.lemma}")
                if key not in shipped_keys:
                    continue
                idx[arabic.skeleton(g.form)][key] += 1
                idx[arabic.skeleton(toks[w - 1]["arabic"])][key] += 1
    return {sk: c.most_common(1)[0][0] for sk, c in idx.items()}


def build(words, qac, gtaf_en):
    by_key = {w.lemma.key: w for w in words}
    by_pron = defaultdict(list)
    by_skel = defaultdict(list)
    for w in words:
        by_pron[arabic.pronunciation_key(w.arabic)].append(w)
        by_skel[arabic.skeleton(w.arabic)].append(w)
    surface = _surface_index(qac, gtaf_en, set(by_key))
    out, stats = {}, {}
    for version, ref in RELEASES.items():
        old_words = _show(ref, "app/src/main/assets/content/word_frequency.json")["words"]
        word_map, methods = {}, Counter()
        for ow in old_words:
            n = int(ow["id"].split("_")[1])
            old_cat = "PARTICLE" if n <= 173 else ("VERB" if n <= 1652 else "NOUN")
            txt = ow["arabicWord"]
            hit, method = None, None
            for pool, name in ((by_pron.get(arabic.pronunciation_key(txt), []), "exact"),
                               (by_skel.get(arabic.skeleton(txt), []), "skeleton")):
                if pool:
                    same_cat = [w for w in pool if w.category == old_cat] or pool
                    hit, method = max(same_cat, key=lambda w: w.frequency), name
                    break
            if hit is None:
                key = surface.get(arabic.skeleton(txt))
                if key:
                    hit, method = by_key[key], "surface"
            methods[method or "unmapped"] += 1
            if hit:
                word_map[ow["id"]] = hit.id
        exercises = _show(ref, "app/src/main/assets/content/exercises_vocabulary.json")["exercises"]
        lesson_words = defaultdict(list)
        for e in exercises:
            c = e["content"]
            if c.get("type") == "word_intro" and c.get("wordId") in word_map:
                nid = word_map[c["wordId"]]
                if nid not in lesson_words[e["lessonId"]]:
                    lesson_words[e["lessonId"]].append(nid)
        out[str(version)] = {"words": word_map, "lessonWords": dict(lesson_words)}
        stats[version] = dict(methods)
    return out, stats
