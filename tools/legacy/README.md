# Legacy content scripts (superseded)

These one-off ingestion and fix scripts produced the content shipped in v1.0.0/v1.0.1. That content
turned out to be corrupted in ways the scripts could not detect:
- Verbs: only 90 distinct lemmas across 1,479 entries.
- Nouns: 594 distinct across 3,057.
- Many meanings were phrase fragments attached to the wrong word.

They are kept only for history. **Do not run them**, since several overwrite app assets in place.
The content is now built by [`tools/pipeline`](../pipeline/README.md).
