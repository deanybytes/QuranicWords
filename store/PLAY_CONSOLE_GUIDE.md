# Google Play Console – step-by-step for QuranicWords v1.0.0 (versionCode 100012)

Everything you need is in this `store/` folder. Copy text **from the files**, not from chat, so the
characters and limits stay exact. Every text file has already been checked against Play's limits
(`python3 tools/store/build_listing.py` prints the counts).

| Field | Play limit | Ours |
|---|---|---|
| App name | 30 | 25–30 |
| Short description | 80 | 69–79 |
| Full description | 4,000 | 2,038–2,582 |
| Release notes ("What's new"), per language | 500 | 324–420 |

Languages and Play locale codes: English `en-US` (default), Bangla `bn-BD`, Urdu `ur`, Hindi `hi-IN`,
Indonesian `id`, Turkish `tr-TR`, Persian `fa`, French `fr-FR`.

---

## Step 1 – Apply for production (day 14 of closed testing)

**Dashboard → "Apply for production"** (appears after 14 days with 12+ opted-in testers).
Paste the answers from **`store/production-access-answers.md`** – one section per question. They
describe what was actually changed after the testers' report.

Google reviews the application (usually a few days). Steps 2–6 can be prepared now; the release in
Step 6 goes live once production access is granted. Until then you can also roll the same AAB out to
your **closed testing** track so testers get the improvements.

## Step 2 – Main store listing (default language: English)

**Grow users → Store presence → Main store listing**

| Field | Paste from |
|---|---|
| App name | `store/listing/en-US/title.txt` |
| Short description | `store/listing/en-US/short_description.txt` |
| Full description | `store/listing/en-US/full_description.txt` |

**Graphics** (same page, scroll down):

| Asset | File | Spec |
|---|---|---|
| App icon | `store/graphics/icon-512.png` | 512 × 512 PNG, full square (Play rounds it) |
| Feature graphic | `store/graphics/feature-graphic.png` | 1024 × 500 PNG |
| Phone screenshots (8) | `store/graphics/phone/en/01…08-*.png` | 1080 × 1920 (9:16) – upload in number order |
| Tablet screenshots | leave empty | optional; the app is phone-first |

The 8 screenshots cover every tab and feature: 01 tour + Home · 02 word in its ayah · 03 quiz +
matching · 04 **home-screen widgets** · 05 XP/level-up/quests · 06 Progress tab + achievements ·
07 Settings tab + 7 fonts · 08 dark mode + About tab.

Click **Save**.

## Step 3 – Translations of the listing

Same page → **Manage translations → Add your own translations** → tick: Bengali (bn-BD), Urdu (ur),
Hindi (hi-IN), Indonesian (id), Turkish (tr-TR), Persian (fa), French (fr-FR).

For **each** language, select it in the language picker at the top, then paste:
- App name ← `store/listing/<locale>/title.txt`
- Short description ← `store/listing/<locale>/short_description.txt`
- Full description ← `store/listing/<locale>/full_description.txt`

**Screenshots per language** – every language has its own 8 screenshots (the app running in that
language, captions translated):

| Language | Upload these 8 files, in order |
|---|---|
| Bengali `bn-BD` | `store/graphics/phone/bn/01…08` |
| Urdu `ur` | `store/graphics/phone/ur/01…08` |
| Hindi `hi-IN` | `store/graphics/phone/hi/01…08` |
| Indonesian `id` | `store/graphics/phone/id/01…08` |
| Turkish `tr-TR` | `store/graphics/phone/tr/01…08` |
| Persian `fa` | `store/graphics/phone/fa/01…08` |
| French `fr-FR` | `store/graphics/phone/fr/01…08` |

When you select a translation, its Graphics section shows the English images **greyed out** – that
means "using the default language's graphics". In the **Phone screenshots** box for that language,
use its add/replace option to upload the language's own files; once added they override the
English set for that language only. The icon and feature graphic can stay shared (English).
**Save** after each language.

## Step 4 – Store settings

**Grow users → Store presence → Store settings**
- **App category:** Education
- **Tags** (pick up to 5 that Play offers): Education, Language learning, Vocabulary, Reference, Religion
- **Contact email:** contact.deanstalks@gmail.com
- **Website:** https://rmrashahriar.github.io/QuranicWords/

## Step 5 – App content (Policy → App content)

Fill or confirm each item:

| Item | Answer |
|---|---|
| Privacy policy | `https://rmrashahriar.github.io/QuranicWords/privacy.html` |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** (no login) |
| Content rating | Run the questionnaire: category **Reference, News, or Educational**; answer **No** to violence, sexual content, profanity, drugs, gambling, user interaction/sharing, location sharing, purchases → expected rating Everyone / PEGI 3 |
| Target audience | 13 and over is simplest. If you add under-13 age groups, Play's Families policy applies (the app already complies: no ads, no data collection) |
| News app | No |
| Data safety | **Does your app collect or share any of the required user data types? → No.** "Is all of the user data collected by your app encrypted in transit?" → not applicable (no data leaves the device). The app has **no Internet permission** in this build, so this is literally true. Account deletion: not applicable (no accounts) |
| Government app | No |
| Financial features | None |
| Health | None |

## Step 6 – Create the release

**Test and release → Production → Create new release** (or the closed testing track until
production access is granted).

1. **App bundles → Upload** `store/release/QuranicWords-v1.0.0.aab`
   - It already contains the native debug symbols and the R8 mapping (BUNDLE-METADATA), so Play
     decodes crashes automatically. If Play still asks: **App bundle explorer → (this version) →
     Downloads** → upload `QuranicWords-v1.0.0-native-debug-symbols.zip` and
     `QuranicWords-v1.0.0-mapping.txt.gz` (unzip to `mapping.txt` if it wants the plain file).
2. **Release name:** `1.0.0 (100012)`
3. **Release notes:** open `store/release-notes.txt` and paste its **whole** content into the release
   notes box. It is already in Play's `<en-US> … </en-US>` per-language format, every language
   ≤ 500 characters.
4. **Next → review warnings → Save → Send for review** (start with a staged rollout of 20–50% if you
   want to watch crash reports first, then raise it to 100%).

## Step 7 – After publishing

- **Quality → Android vitals:** check crashes/ANRs during the first days.
- **Ratings and reviews:** reply to early reviews; the testers' report recommends collecting feedback.
- The website, the offline HTML dictionary and the APK are also on GitHub:
  https://github.com/rmrashahriar/QuranicWords/releases/tag/v1.0.0

## Checksums

`store/release/SHA256SUMS.txt` lists the SHA-256 of every file in `store/release/`.
