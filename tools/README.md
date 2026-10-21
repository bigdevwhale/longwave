# Narration

`narrate.py` records every line the stations say (`content/ScriptEn.kt`, `content/ScriptRu.kt`) into
`app/src/main/assets/voice/<lang>/<hash>.webm`. The app finds a recording by the SHA-1 of the exact
text (`Narrator.key`); a line without one is skipped and the station plays on, and
`RadioTest.everyLineIsRecorded` fails until it is recorded.

Two voices: lines wrapped in `a("…")` are read by the announcer (Silero `eugene`, Kokoro `bm_george`),
lines in `s("…")` by the soft night voice (Silero `xenia`, Kokoro `af_heart`).

```bash
python -m venv venv
venv/Scripts/pip install torch torchaudio --index-url https://download.pytorch.org/whl/cu124
venv/Scripts/pip install kokoro soundfile imageio-ffmpeg numpy omegaconf

venv/Scripts/python tools/narrate.py                    # record whatever is missing, both languages
venv/Scripts/python tools/narrate.py --lang ru --prune  # also drop recordings of removed lines
venv/Scripts/python tools/narrate.py --sample out/      # compare announcer voices on one line
```

Voices and pace: `LONGWAVE_RU_ANNOUNCER`, `LONGWAVE_EN_ANNOUNCER`, `LONGWAVE_RU_SPEAKER`,
`LONGWAVE_EN_VOICE`, `LONGWAVE_EN_SPEED`, `LONGWAVE_TEMPO` (pitch-preserving slow-down) and
`LONGWAVE_BREATH_MS` (silence between sentences). After changing a voice, delete its recordings and
run again.

Licenses: Silero models are CC BY-NC-SA 4.0 (non-commercial), Kokoro-82M is Apache 2.0.
