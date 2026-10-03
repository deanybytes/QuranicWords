# store/ – everything for the Google Play release

Start with **[PLAY_CONSOLE_GUIDE.md](PLAY_CONSOLE_GUIDE.md)** – it says what to paste or upload where.

| Path | What |
|---|---|
| `PLAY_CONSOLE_GUIDE.md` | Step-by-step Play Console walkthrough, with character limits |
| `production-access-answers.md` | Answers for "Apply for production" |
| `listing/<locale>/title.txt` | App name (≤ 30) – 8 languages |
| `listing/<locale>/short_description.txt` | Short description (≤ 80) |
| `listing/<locale>/full_description.txt` | Full description (≤ 4,000) |
| `listing/<locale>/release_notes.txt` | What's new (≤ 500) |
| `release-notes.txt` | All release notes in Play's `<locale>…</locale>` paste format |
| `graphics/icon-512.png` | Play app icon, 512 × 512 |
| `graphics/feature-graphic/<lang>.png` | Feature graphic, 1024 × 500, one per language |
| `graphics/phone/<lang>/` | 8 annotated phone screenshots per language, 1080 × 1920 |
| `raw-screens/` | The unedited app captures the screenshots are made from |
| `release/` | AAB, APK, offline HTML, native debug symbols, R8 mapping, SHA256SUMS (not in git) |

Regenerate: `python3 tools/store/build_listing.py` (text, checks limits) and
`python3 tools/store/make_store_graphics.py` (images).
