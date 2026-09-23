/**
 * QuranicWords — Audio Engine & Arabic Speech Synthesizer
 */

class AudioService {
  constructor() {
    this.synth = window.speechSynthesis || null;
    this.arabicVoice = null;
    this.initVoices();
    if (this.synth && this.synth.onvoiceschanged !== undefined) {
      this.synth.onvoiceschanged = () => this.initVoices();
    }
  }

  initVoices() {
    if (!this.synth) return;
    const voices = this.synth.getVoices();
    this.arabicVoice = voices.find(v => v.lang.startsWith('ar') || v.name.toLowerCase().includes('arabic')) || null;
  }

  speakArabic(text) {
    if (!text) return;
    if (!this.synth) {
      console.warn('SpeechSynthesis not supported in this browser');
      return;
    }

    // Cancel any ongoing utterance
    this.synth.cancel();

    const cleanText = text.replace(/[\(\)0-9]/g, '').trim();
    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.lang = 'ar-SA';
    utterance.rate = 0.85; // Slightly slower for clear Tajweed/pronunciation
    utterance.pitch = 1.0;

    if (this.arabicVoice) {
      utterance.voice = this.arabicVoice;
    }

    this.synth.speak(utterance);
  }
}

export const audioService = new AudioService();
