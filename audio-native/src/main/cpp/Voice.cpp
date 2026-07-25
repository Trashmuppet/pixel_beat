// Monochrome Beat — procedural drum voice implementations.
//
// Each voice renders into the supplied mono buffer. All math uses
// float32; the mixer applies stereo pan and master limiter downstream.

#include "Voice.h"
#include <cmath>
#include <algorithm>

namespace monochrome_beat::audio {

namespace {

constexpr float kTwoPi = 6.28318530717958647692f;

// Cheap procedural noise via xorshift32 → uniform [-1, 1].
inline float xorshift32(uint32_t& state) noexcept {
    uint32_t x = state;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    state = x;
    constexpr float inv = 1.0f / float(UINT32_MAX);
    return (float(x) * inv) * 2.0f - 1.0f;
}

inline float dbToGain(float db) noexcept {
    return std::pow(10.0f, db * 0.05f);
}

inline float expDecay(float ampCoef, int samples) noexcept {
    return std::exp(ampCoef * float(samples));
}

inline void advancePhase(float& phase, float freq, int frames, float sampleRate) noexcept {
    phase += frames * freq / sampleRate;
    phase -= std::floor(phase);
}

}  // namespace

// --- Kick ------------------------------------------------------------------
//
// 60 Hz sine with downward pitch sweep and exponential decay. The
// "kick" identity is dominated by a fast (≈40 ms) amp decay.

void KickVoice::trigger(int64_t when, float velocity) noexcept {
    triggered_ = true;
    start_ = when;
    velocity_ = velocity;
    // amp coefficient: -1 / (3 ms * sampleRate) — fast decay.
    amp_ = -1.0f / (0.003f * 48000.0f);
    freq_ = 110.0f;        // start frequency
}

void KickVoice::render(float* out, int frames, int64_t now) noexcept {
    if (!triggered_) return;
    constexpr float sampleRate = 48000.0f;

    int64_t age = now - start_;
    if (age >= int64_t(frames) + 1) {
        // Window fully past — finish.
        triggered_ = false;
    }
    // Render only the slice inside the buffer.
    int64_t startOffset = std::max<int64_t>(0, start_ - now);
    int64_t endOffset = std::min<int64_t>(frames, startOffset + frames);
    for (int64_t i = startOffset; i < endOffset; ++i) {
        float localAge = float((now + i) - start_);
        float env = std::exp(amp_ * localAge);
        // Sweep freq down 110 -> 50 Hz over first 50 ms.
        float f = 110.0f - 60.0f * std::min(1.0f, localAge / 1200.0f);
        phase_ += f / sampleRate;
        phase_ -= std::floor(phase_);
        float s = std::sin(phase_ * kTwoPi);
        out[i] += velocity_ * gain_ * env * s;
    }
}

void KickVoice::reset() noexcept { triggered_ = false; amp_ = 0.0f; freq_ = 80.0f; phase_ = 0.0f; }

// --- Snare -----------------------------------------------------------------
//
// Noise + bandpass-ish (approximated with first-order highpass) + amp
// decay. Includes a 200 Hz body partial that gives snare its tonal
// anchor.

float SnareVoice::nextNoise() noexcept { return xorshift32(rng_state_); }

void SnareVoice::trigger(int64_t when, float velocity) noexcept {
    triggered_ = true;
    start_ = when;
    velocity_ = velocity;
    amp_ = -1.0f / (0.10f * 48000.0f);  // ≈100 ms decay
}

void SnareVoice::render(float* out, int frames, int64_t now) noexcept {
    if (!triggered_) return;
    constexpr float sampleRate = 48000.0f;
    int64_t age = now - start_;
    if (age >= int64_t(frames) + 1) triggered_ = false;

    int64_t startOffset = std::max<int64_t>(0, start_ - now);
    int64_t endOffset = std::min<int64_t>(frames, startOffset + frames);
    for (int64_t i = startOffset; i < endOffset; ++i) {
        float localAge = float((now + i) - start_);
        float env = std::exp(amp_ * localAge);
        float n = nextNoise();
        // 200 Hz body partial + noise.
        float tone = std::sin(float((now + i)) * 200.0f * kTwoPi / sampleRate);
        out[i] += velocity_ * gain_ * env * (0.6f * n + 0.4f * tone);
    }
}

void SnareVoice::reset() noexcept { triggered_ = false; amp_ = 0.0f; }

// --- Closed hat ------------------------------------------------------------
//
// Square-wave + first-order high-pass + fast amp decay (≈30 ms).

void ClosedHatVoice::trigger(int64_t when, float velocity) noexcept {
    triggered_ = true;
    start_ = when;
    velocity_ = velocity;
    amp_ = -1.0f / (0.03f * 48000.0f);
    phase_ = 0.0f;
}

void ClosedHatVoice::render(float* out, int frames, int64_t now) noexcept {
    if (!triggered_) return;
    constexpr float sampleRate = 48000.0f;
    int64_t age = now - start_;
    if (age >= int64_t(frames) + 1) triggered_ = false;

    int64_t startOffset = std::max<int64_t>(0, start_ - now);
    int64_t endOffset = std::min<int64_t>(frames, startOffset + frames);
    for (int64_t i = startOffset; i < endOffset; ++i) {
        float localAge = float((now + i) - start_);
        float env = std::exp(amp_ * localAge);
        // Two-octave square.
        phase_ += 8000.0f / sampleRate;
        phase_ -= std::floor(phase_);
        float sq = phase_ < 0.5f ? 1.0f : -1.0f;
        out[i] += velocity_ * gain_ * env * sq * 0.4f;
    }
}

void ClosedHatVoice::reset() noexcept { triggered_ = false; amp_ = 0.0f; phase_ = 0.0f; }

// --- Open hat --------------------------------------------------------------
//
// Same synth as CH but with a longer decay (≈180 ms) — distinct from
// the V1 spec.

void OpenHatVoice::trigger(int64_t when, float velocity) noexcept {
    triggered_ = true;
    start_ = when;
    velocity_ = velocity;
    amp_ = -1.0f / (0.18f * 48000.0f);
    phase_ = 0.0f;
}

void OpenHatVoice::render(float* out, int frames, int64_t now) noexcept {
    if (!triggered_) return;
    constexpr float sampleRate = 48000.0f;
    int64_t age = now - start_;
    if (age >= int64_t(frames) + 1) triggered_ = false;

    int64_t startOffset = std::max<int64_t>(0, start_ - now);
    int64_t endOffset = std::min<int64_t>(frames, startOffset + frames);
    for (int64_t i = startOffset; i < endOffset; ++i) {
        float localAge = float((now + i) - start_);
        float env = std::exp(amp_ * localAge);
        phase_ += 8000.0f / sampleRate;
        phase_ -= std::floor(phase_);
        float sq = phase_ < 0.5f ? 1.0f : -1.0f;
        out[i] += velocity_ * gain_ * env * sq * 0.3f;
    }
}

void OpenHatVoice::reset() noexcept { triggered_ = false; amp_ = 0.0f; phase_ = 0.0f; }

// --- Clap ------------------------------------------------------------------
//
// Series of noise bursts at 12 ms intervals + tail body.

float ClapVoice::nextNoise() noexcept { return xorshift32(rng_state_); }

void ClapVoice::trigger(int64_t when, float velocity) noexcept {
    retrig_ = 3;
    last_ = when;
    velocity_ = velocity;
}

void ClapVoice::render(float* out, int frames, int64_t now) noexcept {
    constexpr float sampleRate = 48000.0f;
    if (retrig_ == 0 && amp_ < 1e-4f) return;
    constexpr int64_t kBurst = int64_t(0.011f * 48000.0f); // 11 ms between bursts
    constexpr float kAmpDecay = -1.0f / (0.20f * 48000.0f);

    for (int i = 0; i < frames; ++i) {
        // Trigger next burst if elapsed.
        int64_t t = now + i;
        if (retrig_ > 0 && t - last_ > kBurst) {
            retrig_--;
            last_ = t;
            amp_ = 0.7f * float(retrig_ + 1);
        }
        if (amp_ <= 0.0f) { amp_ = 0.0f; continue; }
        amp_ *= std::exp(kAmpDecay);
        out[i] += velocity_ * gain_ * amp_ * nextNoise();
    }
}

void ClapVoice::reset() noexcept { retrig_ = 0; amp_ = 0.0f; last_ = -1000000; }

// --- Tom -------------------------------------------------------------------
//
// Sine with short downward pitch sweep; tuned per voice instance.

void TomVoice::trigger(int64_t when, float velocity) noexcept {
    triggered_ = true;
    start_ = when;
    velocity_ = velocity;
    freq_ = 220.0f;
    amp_ = -1.0f / (0.25f * 48000.0f);
    phase_ = 0.0f;
}

void TomVoice::render(float* out, int frames, int64_t now) noexcept {
    if (!triggered_) return;
    constexpr float sampleRate = 48000.0f;
    int64_t age = now - start_;
    if (age >= int64_t(frames) + 1) triggered_ = false;

    int64_t startOffset = std::max<int64_t>(0, start_ - now);
    int64_t endOffset = std::min<int64_t>(frames, startOffset + frames);
    for (int64_t i = startOffset; i < endOffset; ++i) {
        float localAge = float((now + i) - start_);
        float env = std::exp(amp_ * localAge);
        float f = freq_ - 60.0f * std::min(1.0f, localAge / 2400.0f);
        phase_ += f / sampleRate;
        phase_ -= std::floor(phase_);
        float s = std::sin(phase_ * kTwoPi);
        out[i] += velocity_ * gain_ * env * s;
    }
}

void TomVoice::reset() noexcept { triggered_ = false; amp_ = 0.0f; freq_ = 200.0f; phase_ = 0.0f; }

}  // namespace monochrome_beat::audio
