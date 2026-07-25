// Monochrome Beat — mixer implementation.
//
// Sum each voice into stereo L/R using constant-power panning:
//   L = cos((pan + 1) * π/4)
//   R = sin((pan + 1) * π/4)
// Then run a 1-frame lookahead peak limiter to keep the master in
// range. Real Phase-6 work replaces this with a proper L/R compressor
// and DC blocker.

#include "Mixer.h"
#include <algorithm>
#include <array>
#include <cmath>

namespace monochrome_beat::audio {

namespace {
constexpr float kHalfPi = 1.57079632679489661923f;
}

void Mixer::mix(const std::array<Voice*, kVoiceCount>& voices,
                float* outL, float* outR,
                int frames, int64_t now) noexcept {
    if (frames <= 0) return;

    // Scratch per-voice buffer; reused each call.
    static thread_local std::array<float, kScratchFrames> scratch{};
    const int stackFrames = std::min(frames, kScratchFrames);

    for (int v = 0; v < kVoiceCount; ++v) {
        Voice* voice = voices[v];
        if (voice == nullptr) continue;
        std::fill(scratch.begin(), scratch.begin() + stackFrames, 0.0f);
        voice->render(scratch.data(), stackFrames, now);

        const float pan = voice->pan();
        const float Lp = std::cos((pan + 1.0f) * kHalfPi * 0.5f);
        const float Rp = std::sin((pan + 1.0f) * kHalfPi * 0.5f);
        const float gain = voice->gain();

        for (int i = 0; i < stackFrames; ++i) {
            float s = scratch[i] * gain;
            outL[i] += Lp * s;
            outR[i] += Rp * s;
        }
    }

    // Master peak limiter. Naive single-sample lookahead: scan ahead
    // for the peak in the chunk, scale back if needed. Cheap and
    // allocation-free; exposed for replacement in Phase 6.
    float peakL = 0.0f, peakR = 0.0f;
    for (int i = 0; i < stackFrames; ++i) {
        peakL = std::max(peakL, std::abs(outL[i]));
        peakR = std::max(peakR, std::abs(outR[i]));
    }
    const float scaleL = peakL > masterLimit_ ? masterLimit_ / peakL : 1.0f;
    const float scaleR = peakR > masterLimit_ ? masterLimit_ / peakR : 1.0f;
    if (scaleL < 1.0f || scaleR < 1.0f) {
        for (int i = 0; i < stackFrames; ++i) {
            outL[i] *= scaleL;
            outR[i] *= scaleR;
        }
    }
}

}  // namespace monochrome_beat::audio
