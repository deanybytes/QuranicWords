// Pronunciation: prefer the bundled MP3 recording (word.au); fall back to the Web Speech API only
// when the device actually has an Arabic voice. Otherwise audio is reported unavailable.

const synth = typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : null;
let arabicVoice = null;
const voiceListeners = new Set();
let player = null;

function refreshVoices() {
  if (!synth) return;
  try {
    const had = Boolean(arabicVoice);
    arabicVoice = synth.getVoices().find((v) => /^ar(\b|[-_])/i.test(v.lang)) || null;
    if (!had && arabicVoice) for (const fn of voiceListeners) fn();
  } catch {
    arabicVoice = null;
  }
}

if (synth) {
  refreshVoices();
  try { synth.addEventListener('voiceschanged', refreshVoices); } catch { /* old browsers */ }
}

/** Called once an Arabic speech voice becomes available (voices load asynchronously). */
export function onVoicesReady(fn) {
  voiceListeners.add(fn);
}

/** 'mp3' | 'tts' | null */
export function audioKind(word) {
  if (word && word.au) return 'mp3';
  if (arabicVoice) return 'tts';
  return null;
}

export function canPlay(word) {
  return audioKind(word) !== null;
}

function speak(text) {
  return new Promise((resolve, reject) => {
    if (!synth || !arabicVoice) { reject(new Error('no-voice')); return; }
    synth.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.voice = arabicVoice;
    u.lang = arabicVoice.lang;
    u.rate = 0.8;
    u.onend = () => resolve();
    u.onerror = (e) => reject(e.error || e);
    synth.speak(u);
  });
}

/** Plays a word. Resolves when playback starts (MP3) or finishes (TTS); rejects if impossible. */
export async function playWord(word) {
  if (!word) throw new Error('no-word');
  if (synth) synth.cancel();
  if (word.au) {
    try {
      if (!player) player = new Audio();
      player.pause();
      player.src = word.au;
      await player.play();
      return;
    } catch (e) {
      if (!arabicVoice) throw e;
    }
  }
  await speak(word.ar);
}

export function stopAudio() {
  try { if (player) player.pause(); } catch { /* ignore */ }
  try { if (synth) synth.cancel(); } catch { /* ignore */ }
}
