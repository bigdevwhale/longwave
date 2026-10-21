"""
Records every line the Longwave stations say with open neural voices: Silero (Russian) and
Kokoro (English). Lines wrapped in a("...") in content/Script*.kt are read by the announcer, a
lower, even radio voice; lines in s("...") by the soft night voice. Output: app/src/main/assets/voice/<lang>/<hash>.webm,
Opus in WebM, which Android plays natively from 5.0. The hash must match Narrator.key() in the app:
the first 16 hex digits of SHA-1 over the trimmed UTF-8 text.

Lines already recorded are skipped, so editing a text and re-running only records what changed.
Recordings whose text no longer exists are removed with --prune.

    python tools/narrate.py [--lang ru|en] [--only "substring"] [--prune] [--sample DIR]

Needs: torch, kokoro, soundfile, imageio-ffmpeg (see tools/README.md).
"""
import argparse
import hashlib
import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'app', 'longwave', 'content')
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'voice')

# Voices, chosen by ear: calm, warm, unhurried.
RU_SPEAKER = os.environ.get('LONGWAVE_RU_SPEAKER', 'xenia')
# A slightly lower, warmer Russian voice: Silero's own pitch, then a gentle shift in semitones.
RU_PITCH = os.environ.get('LONGWAVE_RU_PITCH', 'medium')
RU_SEMITONES = float(os.environ.get('LONGWAVE_RU_SEMITONES', '0'))
EN_VOICE = os.environ.get('LONGWAVE_EN_VOICE', 'af_heart')
# The announcer: the shipping forecast is read by a man with a British voice.
RU_ANNOUNCER = os.environ.get('LONGWAVE_RU_ANNOUNCER', 'eugene')
EN_ANNOUNCER = os.environ.get('LONGWAVE_EN_ANNOUNCER', 'bm_george')
EN_SPEED = float(os.environ.get('LONGWAVE_EN_SPEED', '0.8'))
# Extra pitch-preserving slow-down applied to every recording (1.0 = none).
TEMPO = float(os.environ.get('LONGWAVE_TEMPO', '0.9'))
BREATH_MS = int(os.environ.get('LONGWAVE_BREATH_MS', '1000'))
BITRATE = '24k'

STRING = r'"((?:[^"\\]|\\.)*)"'


def unescape(s):
    return s.encode('utf-8').decode('unicode_escape').encode('latin-1').decode('utf-8')


def lines(lang):
    """Every line in one language as (text, announcer?) pairs."""
    suffix = 'Ru' if lang == 'ru' else 'En'
    body = open(os.path.join(CONTENT, f'Script{suffix}.kt'), encoding='utf-8').read()
    out, seen = [], set()
    for who, raw in re.findall(r'(?<![A-Za-z])([as])\(' + STRING + r'\)', body):
        t = unescape(raw).strip()
        if t and t not in seen:
            seen.add(t)
            out.append((t, who == 'a'))
    return out


def key(text):
    return hashlib.sha1(text.strip().encode('utf-8')).hexdigest()[:16]


class Ru:
    def __init__(self):
        import torch
        self.torch = torch
        self.model, _ = torch.hub.load('snakers4/silero-models', 'silero_tts', language='ru', speaker='v4_ru', trust_repo=True)
        self.model.to(torch.device('cuda' if torch.cuda.is_available() else 'cpu'))
        self.rate = 48000

    def __call__(self, text, speaker=RU_SPEAKER):
        # Silero reads SSML: a slower, softer delivery and a breath between sentences.
        body = re.sub(r'([.!?…])\s+', rf'\1 <break time="{BREATH_MS}ms"/> ', escape(text))
        ssml = f'<speak><prosody rate="slow" pitch="{RU_PITCH}">{body}</prosody></speak>'
        audio = self.model.apply_tts(ssml_text=ssml, speaker=speaker, sample_rate=self.rate, put_accent=True, put_yo=True)
        return audio.cpu().numpy(), self.rate


class En:
    def __init__(self):
        from kokoro import KPipeline
        self.pipes = {}
        self.rate = 24000

    def __call__(self, text, voice=EN_VOICE):
        import numpy as np
        from kokoro import KPipeline
        code = 'a' if voice.startswith('a') else 'b'
        if code not in self.pipes:
            self.pipes[code] = KPipeline(lang_code=code)
        parts = []
        for _, _, audio in self.pipes[code](text, voice=voice, speed=EN_SPEED, split_pattern=r'(?<=[.!?…])\s+'):
            parts.append(audio.numpy() if hasattr(audio, 'numpy') else audio)
            parts.append(np.zeros(int(self.rate * BREATH_MS / 1000), dtype='float32'))
        return np.concatenate(parts[:-1]) if parts else np.zeros(1, dtype='float32'), self.rate


def voice_filter(rate, semitones):
    """Slow-down and pitch shift without changing each other: resample for pitch, atempo for pace."""
    f = 2 ** (semitones / 12)
    shift = f'asetrate={int(rate * f)},aresample={rate},' if semitones else ''
    tempo = TEMPO / f if semitones else TEMPO
    return f'{shift}atempo={tempo:.4f},loudnorm=I=-19:TP=-1.5:LRA=11'


def escape(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def encode(samples, rate, path, semitones=0.0):
    """Float PCM to mono Opus/WebM, with soft edges so lines never click."""
    import numpy as np
    import soundfile as sf
    import imageio_ffmpeg
    x = np.asarray(samples, dtype='float32')
    fade = min(len(x) // 4, int(rate * 0.015))
    if fade:
        x[:fade] *= np.linspace(0, 1, fade)
        x[-fade:] *= np.linspace(1, 0, fade)
    peak = float(np.max(np.abs(x))) or 1.0
    x = x * min(1.0, 0.89 / peak)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        sf.write(tmp.name, x, rate)
    try:
        subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), '-y', '-loglevel', 'error', '-i', tmp.name,
                        '-ac', '1', '-af', voice_filter(rate, semitones), '-ar', '48000', '-c:a', 'libopus', '-b:a', BITRATE, '-application', 'voip', path], check=True)
    finally:
        os.unlink(tmp.name)


def write_durations(folder):
    """durations.json: seconds per recording, so the app can show true session lengths."""
    import json
    import imageio_ffmpeg
    from concurrent.futures import ThreadPoolExecutor
    ff = imageio_ffmpeg.get_ffmpeg_exe()

    def length(name):
        out = subprocess.run([ff, '-i', os.path.join(folder, name), '-f', 'null', '-'], capture_output=True, text=True).stderr
        t = re.findall(r'time=(\d+):(\d+):([\d.]+)', out)[-1]
        return name[:-5], round(int(t[0]) * 3600 + int(t[1]) * 60 + float(t[2]), 2)

    names = sorted(f for f in os.listdir(folder) if f.endswith('.webm'))
    with ThreadPoolExecutor(12) as ex:
        data = dict(ex.map(length, names))
    with open(os.path.join(folder, 'durations.json'), 'w', encoding='utf-8') as f:
        json.dump(data, f, separators=(',', ':'), sort_keys=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--lang', choices=['ru', 'en'], action='append')
    ap.add_argument('--only', help='record only lines containing this text')
    ap.add_argument('--prune', action='store_true', help='delete recordings of lines that no longer exist')
    ap.add_argument('--sample', help='write a few test lines in several voices to this folder and stop')
    args = ap.parse_args()

    if args.sample:
        os.makedirs(args.sample, exist_ok=True)
        ru, en = Ru(), En()
        for sp in ('eugene', 'aidar', RU_SPEAKER):
            encode(*ru('Море Тишины. Ветер западный, три — четыре балла. Видимость хорошая.', sp), os.path.join(args.sample, f'ru-{sp}.webm'))
        for v in ('bm_george', 'bm_lewis', 'am_michael', EN_VOICE):
            encode(*en('Sea of Quiet. Westerly, three or four. Visibility good.', v), os.path.join(args.sample, f'en-{v}.webm'))
        print('samples in', args.sample)
        return

    for lang in args.lang or ['ru', 'en']:
        todo = lines(lang)
        folder = os.path.join(OUT, lang)
        os.makedirs(folder, exist_ok=True)
        wanted = {key(t) + '.webm' for t, _ in todo}
        if args.prune:
            for f in os.listdir(folder):
                if f not in wanted:
                    os.remove(os.path.join(folder, f))
        if args.only:
            todo = [(t, an) for t, an in todo if args.only in t]
        missing = [(t, an) for t, an in todo if not os.path.exists(os.path.join(folder, key(t) + '.webm'))]
        print(f'{lang}: {len(todo)} lines, {len(missing)} to record', flush=True)
        if not missing:
            write_durations(folder)
            continue
        engine = Ru() if lang == 'ru' else En()
        for i, (t, announcer) in enumerate(missing, 1):
            voice = (RU_ANNOUNCER if announcer else RU_SPEAKER) if lang == 'ru' else (EN_ANNOUNCER if announcer else EN_VOICE)
            try:
                encode(*engine(t, voice), os.path.join(folder, key(t) + '.webm'), RU_SEMITONES if lang == 'ru' else 0.0)
            except Exception as e:  # one odd line must not stop the batch
                print(f'  ! {t[:60]}: {e}', file=sys.stderr, flush=True)
            if i % 25 == 0:
                print(f'  {i}/{len(missing)}', flush=True)
        write_durations(folder)


if __name__ == '__main__':
    main()
