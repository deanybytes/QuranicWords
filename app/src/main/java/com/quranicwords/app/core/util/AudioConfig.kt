package com.quranicwords.app.core.util

/**
 * Where word-pronunciation audio clips are fetched from. Kept as one named constant (not
 * scattered inline) so it's a one-line change to point at a different host later - see
 * `docs/CONTENT_SOURCES.md` for how these clips were segmented and
 * `tools/ingestion/12_segment_word_audio.py` for the pipeline that produces them.
 *
 * Currently a small public proof-of-concept release (~20 words) rather than the full corpus -
 * `WordFrequencyEntity.audioAssetPath` is only non-null for words that pipeline has actually
 * covered so far; everything else plays nothing, degrading exactly like the rest of this
 * codebase's "flag rather than fake" content gaps (see [AudioPlayer]/[WordAudioRepository]).
 */
object AudioConfig {
    const val WORD_AUDIO_BASE_URL =
        "https://github.com/rmrashahriar/QuranicWords-audio/releases/download/word-audio-sample-v1/"
}
