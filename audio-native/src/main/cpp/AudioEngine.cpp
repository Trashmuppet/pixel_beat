// Monochrome Beat — audio engine implementation.
//
// Owns six procedure-drum voices, a HitQueue lock-free scheduler, the
// Mixer bus, and — when MONOCHROME_BEAT_HAVE_OBOE is set — an Oboe /
// AAudio stream for live playback. The render path is identical for
// live playback and offline export per ADR-005.
//
// Phase 2 corrected:
//  * Oboe start() pulls AudioStream into a shared_ptr then stores it
//    in ObHandle.stream — the prior pseudo-typed-pointer path is gone.
//  * nativeRenderOffline stages samples via a static scratch and uses
//    SetFloatArrayRegion to copy back into the JNI float[].

#include "AudioEngine.h"

#include <android/log.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstring>
#include <memory>
#include <mutex>

#if MONOCHROME_BEAT_HAVE_OBOE
#include <oboe/Oboe.h>
#endif

#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, "PixelBeatAudio", __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, "PixelBeatAudio", __VA_ARGS__)

namespace monochrome_beat::audio {

// -------------------------------------------------------------------------
// Construction / lifecycle
// -------------------------------------------------------------------------

AudioEngineNative::AudioEngineNative(int sampleRate) : sampleRate_(sampleRate) {
    voices_[static_cast<size_t>(VoiceKind::Kick)]      = std::make_unique<KickVoice>();
    voices_[static_cast<size_t>(VoiceKind::Snare)]     = std::make_unique<SnareVoice>();
    voices_[static_cast<size_t>(VoiceKind::ClosedHat)] = std::make_unique<ClosedHatVoice>();
    voices_[static_cast<size_t>(VoiceKind::OpenHat)]   = std::make_unique<OpenHatVoice>();
    voices_[static_cast<size_t>(VoiceKind::Clap)]       = std::make_unique<ClapVoice>();
    voices_[static_cast<size_t>(VoiceKind::Tom)]       = std::make_unique<TomVoice>();

    for (auto& v : voices_) {
        if (v) v->setPan(0.0f);
    }
    mixer_.setMasterLimit(0.95f);
}

AudioEngineNative::~AudioEngineNative() { stop(); }

// -------------------------------------------------------------------------
// Real-time path (Oboe / AAudio)
// -------------------------------------------------------------------------

#if MONOCHROME_BEAT_HAVE_OBOE

class ObStreamCallback final : public oboe::AudioStreamCallback {
public:
    explicit ObStreamCallback(AudioEngineNative* engine) : engine_(engine) {}
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* /* stream */,
        void* audioData,
        int32_t numFrames) override {
        auto* out = static_cast<float*>(audioData);
        engine_->renderBlock(out, out + numFrames, numFrames);
        return oboe::DataCallbackResult::Continue;
    }
    void onErrorAfterClose(
        oboe::AudioStream* /* stream */,
        oboe::Result error) override {
        ALOGE("AudioStream error: %s", oboe::convertToText(error));
    }
private:
    AudioEngineNative* engine_;
};

struct AudioEngineNative::ObHandle {
    std::shared_ptr<oboe::AudioStream> stream;
    std::unique_ptr<ObStreamCallback>  callback;
};

bool AudioEngineNative::start() {
    if (running_.load()) return true;
    if (!ob_) ob_ = std::make_unique<ObHandle>();

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setSampleRate(sampleRate_);

    ob_->callback = std::make_unique<ObStreamCallback>(this);
    builder.setCallback(ob_->callback.get());

    oboe::Result result = builder.openStream(ob_->stream);
    if (result != oboe::Result::OK || !ob_->stream) {
        ALOGE("openStream failed: %s", oboe::convertToText(result));
        ob_->stream.reset();
        ob_->callback.reset();
        return false;
    }
    result = ob_->stream->requestStart();
    if (result != oboe::Result::OK) {
        ob_->stream->close();
        ob_->stream.reset();
        ob_->callback.reset();
        return false;
    }
    running_.store(true);
    return true;
}

void AudioEngineNative::stop() {
    if (!running_.load()) {
        if (ob_ && ob_->stream) {
            ob_->stream->close();
            ob_->stream.reset();
        }
        ob_->callback.reset();
        return;
    }
    running_.store(false);
    if (ob_ && ob_->stream) {
        ob_->stream->requestStop();
        ob_->stream->close();
        ob_->stream.reset();
    }
    ob_->callback.reset();
}

#else  // MONOCHROME_BEAT_HAVE_OBOE == 0
//
// Without Oboe the engine's realtime is a stub; offline render still
// works (Phase 5 export ships through `renderOffline`).

bool AudioEngineNative::start() { running_.store(true); return true; }
void AudioEngineNative::stop()  { running_.store(false); }
#endif

// -------------------------------------------------------------------------
// Render path — shared by live playback and offline export
// -------------------------------------------------------------------------

void AudioEngineNative::renderBlock(float* outL, float* outR, int frames) noexcept {
    if (frames <= 0) return;
    const int64_t now = nextRenderTick_.fetch_add(frames);

    TriggerEvent ev;
    while (queue_.pop(ev)) {
        if (ev.sampleTick < now) ev.sampleTick = now;
        if (ev.sampleTick > now + frames) {
            queue_.push(ev);   // re-queue, in this buffer's future
            break;
        }
        if (ev.voiceIndex >= 0 && ev.voiceIndex < Mixer::kVoiceCount) {
            voices_[ev.voiceIndex]->trigger(ev.sampleTick, ev.velocity);
        }
    }

    std::memset(outL, 0, sizeof(float) * frames);
    std::memset(outR, 0, sizeof(float) * frames);

    mixer_.mix(voices_, outL, outR, frames, now);
}

void AudioEngineNative::renderOffline(float* out, int frames) {
    if (frames <= 0) return;
    constexpr int kChunk = Mixer::kScratchFrames;
    std::unique_ptr<float[]> leftBuffer  = std::make_unique<float[]>(kChunk);
    std::unique_ptr<float[]> rightBuffer = std::make_unique<float[]>(kChunk);

    int rendered = 0;
    while (rendered < frames) {
        const int chunk = std::min(kChunk, frames - rendered);
        renderBlock(leftBuffer.get(), rightBuffer.get(), chunk);
        for (int i = 0; i < chunk; ++i) {
            // -3 dB pan-summed to keep headroom before WAV / MP4 encode.
            out[rendered + i] = (leftBuffer[i] + rightBuffer[i]) * 0.7071067811865475f;
        }
        rendered += chunk;
    }
}

// -------------------------------------------------------------------------
// Project / schedule API
// -------------------------------------------------------------------------

void AudioEngineNative::loadProject(int64_t projectHash, int32_t /* trackCount */) {
    projectHash_.store(projectHash);
    for (auto& v : voices_) v->reset();
    queue_.reset();
    nextRenderTick_.store(0);
}

void AudioEngineNative::schedule(int32_t voiceIndex,
                                 int64_t samplesUntil,
                                 float velocity) {
    queue_.push({samplesUntil, voiceIndex, velocity});
}

// -------------------------------------------------------------------------
// C export surface for JNI (engine owns itself; JNI gets a non-owning
// pointer-as-handle). Thread-safety: per-handle state is mutable but
// hit-schedule / render are all hit-queue mediated.
// -------------------------------------------------------------------------

extern "C" int64_t mb_aue_create(int sampleRate) {
    auto* engine = new AudioEngineNative(sampleRate);
    return reinterpret_cast<int64_t>(engine);
}

extern "C" int mb_aue_destroy(int64_t handle) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return -1;
    delete engine;
    return 0;
}

extern "C" int mb_aue_start(int64_t handle) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return -1;
    return engine->start() ? 0 : -2;
}

extern "C" int mb_aue_stop(int64_t handle) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return -1;
    engine->stop();
    return 0;
}

extern "C" void mb_aue_loadProject(int64_t handle, int64_t projectHash, int32_t trackCount) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return;
    engine->loadProject(projectHash, trackCount);
}

extern "C" void mb_aue_schedule(int64_t handle,
                                int32_t voiceIndex,
                                int64_t samplesUntil,
                                float velocity) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return;
    engine->schedule(voiceIndex, samplesUntil, velocity);
}

extern "C" int mb_aue_renderOffline(int64_t handle, float* out, int frames) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr || out == nullptr) return -1;
    engine->renderOffline(out, frames);
    return 0;
}

extern "C" int mb_aue_isRunning(int64_t handle) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return 0;
    return engine->isRunning() ? 1 : 0;
}

extern "C" const char* mb_aue_version() {
    return AudioEngineNative::versionString();
}

extern "C" int64_t mb_aue_projectHash(int64_t handle) {
    auto* engine = reinterpret_cast<AudioEngineNative*>(handle);
    if (engine == nullptr) return 0;
    return engine->projectHash();
}

}  // namespace monochrome_beat::audio

// -------------------------------------------------------------------------
// JNI symbols (Phase 2 corrected).
// -------------------------------------------------------------------------

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeCreate(
        JNIEnv* /* env */, jobject /* this */, jint sampleRate) {
    return static_cast<jlong>(monochrome_beat::audio::mb_aue_create(static_cast<int>(sampleRate)));
}

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeDestroy(
        JNIEnv* /* env */, jobject /* this */, jlong handle) {
    return monochrome_beat::audio::mb_aue_destroy(static_cast<int64_t>(handle));
}

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeStart(
        JNIEnv* /* env */, jobject /* this */, jlong handle) {
    return monochrome_beat::audio::mb_aue_start(static_cast<int64_t>(handle));
}

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeStop(
        JNIEnv* /* env */, jobject /* this */, jlong handle) {
    return monochrome_beat::audio::mb_aue_stop(static_cast<int64_t>(handle));
}

JNIEXPORT void JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeLoadProject(
        JNIEnv* /* env */, jobject /* this */,
        jlong handle, jlong projectHash, jint trackCount) {
    monochrome_beat::audio::mb_aue_loadProject(
        static_cast<int64_t>(handle),
        static_cast<int64_t>(projectHash),
        static_cast<int32_t>(trackCount));
}

JNIEXPORT void JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeSchedule(
        JNIEnv* /* env */, jobject /* this */,
        jlong handle, jint voiceIndex, jlong samplesUntil, jfloat velocity) {
    monochrome_beat::audio::mb_aue_schedule(
        static_cast<int64_t>(handle),
        static_cast<int32_t>(voiceIndex),
        static_cast<int64_t>(samplesUntil),
        static_cast<float>(velocity));
}

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeRenderOffline(
        JNIEnv* env, jobject /* this */,
        jlong handle, jfloatArray outArray, jint frames) {
    auto* engine = reinterpret_cast<monochrome_beat::audio::AudioEngineNative*>(handle);
    if (engine == nullptr || outArray == nullptr) return -1;
    constexpr int kScratch = 4096;
    float scratch[kScratch];
    int written = 0;
    while (written < frames) {
        const int chunk = (frames - written) < kScratch ? (frames - written) : kScratch;
        engine->renderOffline(scratch, chunk);
        env->SetFloatArrayRegion(outArray, written, chunk, scratch);
        written += chunk;
    }
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeIsRunning(
        JNIEnv* /* env */, jobject /* this */, jlong handle) {
    return monochrome_beat::audio::mb_aue_isRunning(static_cast<int64_t>(handle)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeVersion(
        JNIEnv* env, jobject /* this */, jlong /* handle */) {
    return env->NewStringUTF(monochrome_beat::audio::AudioEngineNative::versionString());
}

JNIEXPORT jlong JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeProjectHash(
        JNIEnv* /* env */, jobject /* this */, jlong handle) {
    return static_cast<jlong>(monochrome_beat::audio::mb_aue_projectHash(static_cast<int64_t>(handle)));
}

}  // extern "C"
