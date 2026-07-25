// Monochrome Beat — six procedural drum voices.
//
// Each voice is a self-contained synthesis algorithm driven by
// sample-accurate trigger events. Concrete subclasses implement the
// `render` method; `trigger` schedules the next strike.
//
// All voices share these rules:
//   - Allocation-free after construction.
//   - Lock-free / no I/O.
//   - State is held as private members only.
//
// Per `08_AUDIO_ENGINE.md` the V1 voice set is: kick, snare, closed
// hat, open hat, clap, tom.
#pragma once

#include <cstdint>

namespace monochrome_beat::audio {

class Voice {
public:
    Voice() = default;
    virtual ~Voice() = default;

    // Schedule a strike at sample `when` with normalized velocity.
    virtual void trigger(int64_t when, float velocity) noexcept = 0;

    // Render `frames` mono samples into `out`. The voice MUST
    // convert any internal notion of time relative to the global
    // sample cursor; the engine advances `now` per call.
    virtual void render(float* out, int frames, int64_t now) noexcept = 0;

    // Per-voice linear gain in [0, 1].
    virtual void setGain(float gain) noexcept { gain_ = gain; }
    float gain() const noexcept { return gain_; }

    // Pan in [-1, 1] — applied at the Mixer, not the Voice.
    virtual void setPan(float pan) noexcept { pan_ = pan; }
    float pan() const noexcept { return pan_; }

    // Reset internal state between projects. Called when the engine
    // loads a new MBeatProject.
    virtual void reset() noexcept = 0;

protected:
    float gain_ = 0.7f;
    float pan_  = 0.0f;
};

// Concrete voices — each is a tiny procedural synth. Kept together
// for compile-time inlining; the voice is selected by index.
enum class VoiceKind : int32_t {
    Kick = 0,
    Snare = 1,
    ClosedHat = 2,
    OpenHat = 3,
    Clap = 4,
    Tom = 5,
};

// Concrete implementations follow below.

class KickVoice final : public Voice {
public:
    void trigger(int64_t when, float velocity) noexcept override;
    void render(float* out, int frames, int64_t now) noexcept override;
    void reset() noexcept override { triggered_ = false; amp_ = 0.0f; freq_ = 0.0f; phase_ = 0.0f; }
private:
    bool   triggered_ = false;
    int64_t start_ = 0;
    float  amp_ = 0.0f;       // exponential decay coefficient
    float  freq_ = 80.0f;     // base frequency in Hz
    float  phase_ = 0.0f;     // oscillator phase in [0, 1)
    float  velocity_ = 1.0f;
};

class SnareVoice final : public Voice {
public:
    void trigger(int64_t when, float velocity) noexcept override;
    void render(float* out, int frames, int64_t now) noexcept override;
    void reset() noexcept override { triggered_ = false; amp_ = 0.0f; }
private:
    bool   triggered_ = false;
    int64_t start_ = 0;
    float  amp_ = 0.0f;
    float  velocity_ = 1.0f;
    uint32_t rng_state_ = 0xDEADBEEFu;
    float nextNoise() noexcept;
};

class ClosedHatVoice final : public Voice {
public:
    void trigger(int64_t when, float velocity) noexcept override;
    void render(float* out, int frames, int64_t now) noexcept override;
    void reset() noexcept override { triggered_ = false; amp_ = 0.0f; phase_ = 0.0f; }
private:
    bool   triggered_ = false;
    int64_t start_ = 0;
    float  amp_ = 0.0f;
    float  phase_ = 0.0f;
    float  velocity_ = 1.0f;
};

class OpenHatVoice final : public Voice {
public:
    void trigger(int64_t when, float velocity) noexcept override;
    void render(float* out, int frames, int64_t now) noexcept override;
    void reset() noexcept override { triggered_ = false; amp_ = 0.0f; phase_ = 0.0f; }
private:
    bool   triggered_ = false;
    int64_t start_ = 0;
    float  amp_ = 0.0f;
    float  phase_ = 0.0f;
    float  velocity_ = 1.0f;
};

class ClapVoice final : public Voice {
public:
    void trigger(int64_t when, float velocity) noexcept override;
    void render(float* out, int frames, int64_t now) noexcept override;
    void reset() noexcept override { retrig_ = 0; amp_ = 0.0f; }
private:
    int   retrig_ = 0;
    float amp_ = 0.0f;
    int64_t last_ = -1000000;
    float velocity_ = 1.0f;
    uint32_t rng_state_ = 0xBADC0FFEEu;
    float nextNoise() noexcept;
};

class TomVoice final : public Voice {
public:
    void trigger(int64_t when, float velocity) noexcept override;
    void render(float* out, int frames, int64_t now) noexcept override;
    void reset() noexcept override { triggered_ = false; amp_ = 0.0f; freq_ = 0.0f; phase_ = 0.0f; }
private:
    bool   triggered_ = false;
    int64_t start_ = 0;
    float  amp_ = 0.0f;
    float  freq_ = 200.0f;
    float  phase_ = 0.0f;
    float  velocity_ = 1.0f;
};

}  // namespace monochrome_beat::audio
