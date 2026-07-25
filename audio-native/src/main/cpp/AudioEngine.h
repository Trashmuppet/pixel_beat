// Monochrome Beat — audio engine top-level wrapper.
//
// Composes the six procedural voices, the HitQueue lock-free
// scheduler, the Mixer bus, and an Oboe / AAudio stream.
//
// JNI symbols:
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeCreate
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeDestroy
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeStart
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeStop
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeLoadProject
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeSchedule
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeRenderOffline
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeIsRunning
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeVersion
//   Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeProjectHash

#pragma once

#include <array>
#include <cstdint>
#include <memory>
#include <mutex>

#include "HitQueue.h"
#include "Mixer.h"
#include "Voice.h"

namespace monochrome_beat::audio {

class AudioEngineNative {
public:
    AudioEngineNative(int sampleRate);
    ~AudioEngineNative();

    bool start();        // opens Oboe / AAudio stream, starts the audio thread
    void stop();

    void loadProject(int64_t projectHash, int32_t trackCount);
    void schedule(int32_t voiceIndex, int64_t samplesUntil, float velocity);

    bool isRunning() const noexcept { return running_.load(); }
    int sampleRate() const noexcept { return sampleRate_; }
    int64_t projectHash() const noexcept { return projectHash_; }

    // Render `frames` mono samples into `out` *without* opening a real
    // audio stream. Used by offline export (Phase 5). Sample-accurate
    // so identical inputs produce byte-identical outputs.
    void renderOffline(float* out, int frames);

    static const char* versionString() { return "audio_native/0.2.0"; }

private:
    // Internal helper called from the audio callback every frame.
    void renderBlock(float* outL, float* outR, int frames) noexcept;

    int                                  sampleRate_ = 48000;
    std::atomic<bool>                    running_{false};
    std::atomic<int64_t>                 projectHash_{0};

    std::array<std::unique_ptr<Voice>, Mixer::kVoiceCount> voices_;
    Mixer                                mixer_;
    HitQueue                             queue_;

    std::atomic<int64_t>                 nextRenderTick_{0};

    // Oboe integration gated behind the discovery flag from CMakeLists.
#if MONOCHROME_BEAT_HAVE_OBOE
    struct ObHandle;
    std::unique_ptr<ObHandle> ob_;
#endif
};

// C entry points exported for JNI. Defined in AudioEngine.cpp.
extern "C" {
int64_t mb_aue_create(int sampleRate);
int     mb_aue_destroy(int64_t handle);
int     mb_aue_start(int64_t handle);
int     mb_aue_stop(int64_t handle);
void    mb_aue_loadProject(int64_t handle, int64_t projectHash, int32_t trackCount);
void    mb_aue_schedule(int64_t handle, int32_t voiceIndex, int64_t samplesUntil, float velocity);
int     mb_aue_renderOffline(int64_t handle, float* out, int frames);
int     mb_aue_isRunning(int64_t handle);
const char* mb_aue_version();
int64_t mb_aue_projectHash(int64_t handle);
}

}  // namespace monochrome_beat::audio
