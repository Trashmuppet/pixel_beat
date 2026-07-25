// Monochrome Beat — audio engine JNI stub.
//
// Phase 0 keeps this intentionally small so the multi-ABI
// externalNativeBuild pipeline is real and the libaudio_native.so is
// shipped in the APK from day one. The real Oboe / AAudio open() call
// and lock-free, allocation-free render callback land in Phase 2
// (`02_ROADMAP.md`, Phase 2 — Native drum synthesis).

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "PixelBeatAudio"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// Internal engine state placeholder. Phase 2 replaces this with the
// Oboe/AAudio AudioStream and the voice / mixer graph from
// `08_AUDIO_ENGINE.md`.
struct EngineState {
    bool started = false;
};

EngineState g_engine{};

}  // namespace

extern "C" {

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeInit(
        JNIEnv * /* env */,
        jobject /* this */) {
    ALOGI("nativeInit");
    g_engine = EngineState{};
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeStart(
        JNIEnv * /* env */,
        jobject /* this */) {
    ALOGI("nativeStart");
    if (g_engine.started) {
        return 0;
    }
    // Phase 2 hook: open AAudio / Oboe stream here, hand off the
    // allocation-free render callback, then set started = true.
    g_engine.started = true;
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeStop(
        JNIEnv * /* env */,
        jobject /* this */) {
    ALOGI("nativeStop");
    g_engine.started = false;
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeIsRunning(
        JNIEnv * /* env */,
        jobject /* this */) {
    return g_engine.started ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_trashmuppet_pixelbeat_audio_AudioEngine_nativeVersion(
        JNIEnv * env,
        jobject /* this */) {
    return env->NewStringUTF("audio_native/0.1.0");
}

}  // extern "C"
