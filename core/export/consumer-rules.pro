# Keep :core:export public encoder classes intact so consumers that
# reflect into MediaMuxer's surface configuration don't accidentally
# lose the symbol under R8.
-keep class com.trashmuppet.pixelbeat.core.export.audio.** { *; }
-keep class com.trashmuppet.pixelbeat.core.export.video.** { *; }
