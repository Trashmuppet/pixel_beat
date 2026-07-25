# Monochrome Beat — R8 / ProGuard rules (Phase 6).
#
# Rules encoded here keep the structural integrity of the project
# across:
#  - JNI bridges (Phase 2 audio engine — `external fun` symbols).
#  - kotlinx.serialization runtime reflection on @Serializable types.
#  - android.media.* reflective lookups in MediaCodec/MediaMuxer.
#  - Kotlin Metadata for IDE run-mode.
#
# Anything not explicitly kept is fair game for R8 tree-shaking.

# --- Keep audio JNI bridge intact ---------------------------------------
-keep class com.trashmuppet.pixelbeat.audio.AudioEngine { *; }
-keep class com.trashmuppet.pixelbeat.audio.AudioEngine$Companion { *; }
-keepclassmembers class com.trashmuppet.pixelbeat.audio.AudioEngine {
    native <methods>;
}

# --- Keep kotlinx.serialization @Serializable contracts intact ----------
# kotlinx.serialization relies on Companion serialisers + $serializer
# companions; R8 must not strip them or we lose round-trip ability.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.trashmuppet.pixelbeat.** {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class com.trashmuppet.pixelbeat.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.trashmuppet.pixelbeat.core.model.** {
    *;
}

# --- MediaCodec / MediaMuxer / EGL -------------------------------------
# Hardware codec selection walks `MediaCodecList` reflectively on
# some OEM stacks. Keep public introspection helpers.
-keep class android.media.MediaCodecInfo { *; }
-keep class android.media.MediaFormat { *; }
-dontwarn android.media.MediaCodec$**

# --- Export pipeline keep rules ----------------------------------------
-keep class com.trashmuppet.pixelbeat.core.export.audio.** { *; }
-keep class com.trashmuppet.pixelbeat.core.export.video.** { *; }

# --- General guidance ---------------------------------------------------
# Compose Compiler reports stay lean by relying on the BOM contract.
# Strip `Log.*` calls in release (AGP's `proguard-android-optimize.txt`
# already does this when remapped; we don't override).
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
