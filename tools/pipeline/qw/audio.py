"""Re-attaches the bundled pronunciation clips (Google TTS of the commit-c21e5cdd word list) to
the rebuilt words. A clip is used only when its source text, lightly normalized, is *exactly*
the new word's text - so what the learner hears is what they read."""
import json
import subprocess

from . import arabic, config

AUDIO_COMMIT = "c21e5cdd"


def load_clip_texts():
    raw = subprocess.run(
        ["git", "-C", str(config.ROOT), "show", f"{AUDIO_COMMIT}:app/src/main/assets/content/word_frequency.json"],
        capture_output=True, check=True,
    ).stdout
    data = json.loads(raw)
    entries = data["words"] if isinstance(data, dict) else data
    return {e["audioAssetPath"]: e["arabicWord"] for e in entries if e.get("audioAssetPath")}


def assign(words):
    clips = load_clip_texts()
    by_text = {}
    for path, txt in sorted(clips.items()):
        if (config.ROOT / "app" / "src" / "main" / "assets" / path).exists():
            by_text.setdefault(arabic.pronunciation_key(txt), set()).add((txt, path))
    matched = 0
    for w in words:
        found = by_text.get(arabic.pronunciation_key(w.arabic))
        # Several clips are fine only if they were all synthesized from the very same text.
        w.audio = min(p for _, p in found) if found and len({t for t, _ in found}) == 1 else None
        matched += w.audio is not None
    used = {w.audio for w in words if w.audio}
    unused = sorted(set(clips) - used)
    return matched, unused
