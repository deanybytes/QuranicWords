# Production access – answers (Play Console → Apply for production)

Written to match what was actually done in this release. Adjust anything that differs for you
(for example, if you also invited friends, family or a study circle, add that to answer 1).

---

**1. How did you recruit users for your closed test?**

We used a paid testing provider (Testers Community) to get a group of testers on many different
devices and Android versions. We also shared the test with people from our target audience – Qur'an
learners and Arabic students – so we would hear from real users as well.

**2. How easy was it to recruit testers for your app?**

Easy

**3. Describe the engagement you received from testers during your closed test.**

Testers used the app throughout the 14 days: they completed lessons, quizzes and reviews, kept
streaks, added the home-screen widgets and tried the different languages, themes and Qur'an fonts.
They sent a written report covering performance on different devices, usability and the store
listing. No crashes or functional bugs were found; their suggestions were about first-time guidance,
the store listing and presentation.

**4. Provide a summary of the feedback that you received from testers. Include how you collected the
feedback.**

Feedback was collected through the testing provider's structured report after testing on several
devices and SDK versions, plus direct messages from testers. The app performed well on all devices
with no crashes. The main suggestions were: an interactive walkthrough for new users, an exit
confirmation, more keyword-rich store descriptions (ASO) and screenshots that show the app's real
features, such as the widgets and the exercises.

**5. Who is the intended audience for your app?**

Anyone who wants to understand the Qur'an in Arabic: students of the Qur'an, hifz students, new
Muslims, parents and children, and teachers. The app is available in 8 languages (English, Bangla,
Urdu, Hindi, Indonesian, Turkish, Persian and French) for learners around the world.

**6. Describe how your app provides value to the users.**

QuranicWords teaches the 3,833 words that make up 97% of the Qur'an's text, in order of frequency.
Every meaning is shown in a complete ayah with only the taught word highlighted, so learners see
exactly how each word is used. Spaced-repetition Daily Review, exercises, streaks, quests and
home-screen widgets help them remember what they learn. It is free, ad-free and works fully
offline, without an account.

**7. How many installs do you expect your app to have in your first year?**

10K – 100K

**8. What changes did you make to your app based on what you learned during your closed test?**

- Rebuilt the new-user walkthrough into an interactive 6-step tour with live previews of each
  feature, a progress bar, step counter and Skip button; it can be replayed from Settings.
- Kept the exit confirmation on by default (Back on Home asks before closing, can be turned off in
  Settings) and lessons resume where the learner left off.
- Rewrote the store listing with clear, keyword-rich descriptions in all 8 languages.
- Created new feature-focused, annotated screenshots showing every tab, the exercises, the Qur'an
  fonts and the home-screen widgets.
- Improved performance and polish: Home now shows a loading state instead of empty numbers on a
  slow start, clearer lesson summary, labelled memory-strength meter and two new Indo-Pak Qur'an
  fonts (Noorani and Hafezi).
- Removed the Internet permission – the app is now fully offline by design.

**9. How did you decide that your app is ready for production?**

After 14 days of closed testing on many devices with no crashes, we addressed all of the testers'
suggestions, then re-ran our full test suite (unit tests, lint and content checks that verify every
word, meaning and verse) and tested the release build again on an emulator, including upgrading from
the previous version with existing progress.

**10. What did you do differently this time?**

We focused on the first-time experience and on presenting the app clearly: an interactive tour for
new users, store descriptions and screenshots that show real features, and localized listings for
every language the app supports. We also verified the content against the full Qur'an text and made
the app fully offline.
