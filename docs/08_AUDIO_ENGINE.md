# Audio Engine

## Purpose
Provide low-latency native drum synthesis.

## Technology
- Kotlin interface
- Oboe / AAudio
- C++ DSP
- 48 kHz internal rate

## Rules
- Allocation-free callback.
- Lock-free callback.
- No storage or UI access.
- Six drum voices for V1.
- Mixer and master limiter.
- Offline renderer uses identical synthesis.

## Performance
- Stable playback.
- No dropped callbacks during normal use.
- Meets release latency budget.
