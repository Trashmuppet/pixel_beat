// Monochrome Beat — bus mixer + master limiter.
//
// Voices render into per-voice mono scratch buffers; the Mixer sums
// them onto the stereo output bus applying constant-power panning,
// then runs a peak limiter so the export never exceeds 0 dBFS.
//
// All buffers are preallocated. No I/O on the audio thread.

#pragma once

#include <array>
#include <cstdint>

#include "Voice.h"

namespace monochrome_beat::audio {

class Mixer {
public:
    static constexpr int kVoiceCount = 6;
    static constexpr int kScratchFrames = 2048;

    Mixer() = default;
    ~Mixer() = default;

    // Apply the per-voice gain + pan from each voice and sum into
    // `outL` / `outR`. Limits the master bus to [-1, 1].
    void mix(const std::array<Voice*, kVoiceCount>& voices,
             float* outL, float* outR,
             int frames, int64_t now) noexcept;

    void setMasterLimit(float limitLinear) noexcept { masterLimit_ = limitLinear; }

private:
    float masterLimit_ = 0.95f;  // ~ -0.45 dB headroom
};

}  // namespace monochrome_beat::audio
