import base64
import json
import os
import time
import urllib.request
import urllib.error

CONTENT_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "content")
AUDIO_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "audio", "words")
WORD_FREQ_PATH = os.path.join(CONTENT_DIR, "word_frequency.json")

VOICE_NAME = "ar-XA-Wavenet-B"  # male
LANGUAGE_CODE = "ar-XA"
API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"


def synthesize(api_key, text):
    body = json.dumps({
        "input": {"text": text},
        "voice": {"languageCode": LANGUAGE_CODE, "name": VOICE_NAME},
        "audioConfig": {"audioEncoding": "MP3"},
    }).encode("utf-8")
    req = urllib.request.Request(
        f"{API_URL}?key={api_key}",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    return base64.b64decode(data["audioContent"])


def main():
    api_key = os.environ.get("GOOGLE_TTS_API_KEY")
    if not api_key:
        raise SystemExit("GOOGLE_TTS_API_KEY not set in environment")

    os.makedirs(AUDIO_DIR, exist_ok=True)
    data = json.load(open(WORD_FREQ_PATH, encoding="utf-8"))
    words = data["words"]

    generated = 0
    skipped = 0
    failed = []

    for i, w in enumerate(words):
        word_id = w["id"]
        arabic = w["arabicWord"]
        asset_rel_path = f"audio/words/{word_id}.mp3"
        out_path = os.path.join(AUDIO_DIR, f"{word_id}.mp3")

        if not os.path.exists(out_path):
            try:
                audio_bytes = synthesize(api_key, arabic)
                with open(out_path, "wb") as f:
                    f.write(audio_bytes)
                generated += 1
                time.sleep(0.05)
            except Exception as e:
                failed.append(f"{word_id} ({arabic}): {e}")
                continue
        else:
            skipped += 1

        w["audioAssetPath"] = asset_rel_path

        if (i + 1) % 100 == 0:
            print(f"{i + 1}/{len(words)} processed ({generated} generated, {skipped} already existed, {len(failed)} failed)")
            with open(WORD_FREQ_PATH, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    with open(WORD_FREQ_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

    print(f"Done. Generated: {generated}, skipped: {skipped}, failed: {len(failed)}")
    if failed:
        with open(os.path.join(os.path.dirname(__file__), "audio_gen_errors.txt"), "w", encoding="utf-8") as f:
            f.write("\n".join(failed))


if __name__ == "__main__":
    main()
