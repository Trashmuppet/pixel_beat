package com.trashmuppet.pixelbeat.core.export.video

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.GLES20
import com.trashmuppet.pixelbeat.core.export.AudioFramesSource
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState
import com.trashmuppet.pixelbeat.scene.runtime.AnimationSystem
import kotlinx.coroutines.yield
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MP4 (H.264 + AAC) MediaCodec / MediaMuxer encoder.
 *
 * Pipeline:
 *   1. Configure video encoder (`H.264`) for a `COLOR_FormatSurface`
 *      input and grab the encoder's `InputSurface` (EGL target).
 *   2. Configure audio encoder (`AAC` if available, else fall back
 *      to PCM-only MP4 — see ADR-005).
 *   3. Drive a `MediaMuxer` over MP4 with one video + one audio
 *      track.
 *   4. Render each scene frame into the video encoder's EGL
 *      surface via GLES2, consuming `AnimationSystem.latestRenderState()`.
 *   5. Drain both encoders into the muxer; report progress every
 *      10 frames.
 *
 * Determinism caveat (locked in ADR-005): the H.264 bitstream is
 * SoC-specific. Two Phones can encode the same pixels into
 * different bytes. We deliberately do NOT promise byte-identical
 * MP4 — only visually-identical (frame count + per-frame grayscale
 * hash). WAV and GIF retain byte-identical determinism.
 */
class Mp4MediaCodecEncoder(
    private val widthPx: Int,
    private val heightPx: Int,
    private val fps: Int = 30,
    private val animationSystem: AnimationSystem
) {

    suspend fun encode(
        project: MBeatProject,
        framesSource: AudioFramesSource,
        outputFile: File,
        progress: suspend (Float, String) -> Unit
    ) {
        val safe = outputFile.also { it.parentFile?.mkdirs() }

        // EGL surface dimensions must be even — MediaCodec H.264
        // rejects odd widths/heights.
        val evenWidth = MediaUtil.evenDim(widthPx)
        val evenHeight = MediaUtil.evenDim(heightPx)
        val bitrate = computeBitrate(evenWidth, evenHeight)

        val videoEncoderName = MediaUtil.selectVideoEncoder()?.name
            ?: return run { progress(0f, "No H.264 encoder available") }
        val audioEncoder = MediaUtil.selectAacEncoder()?.name
        val hasAac = audioEncoder != null

        val muxer = MediaMuxer(safe.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var videoTrackIndex = -1
        var audioTrackIndex = -1

        try {
            // ------------------------------------------------------------------
            // Video encoder
            // ------------------------------------------------------------------
            val videoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, evenWidth, evenHeight
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaUtil.selectColorFormat(
                        MediaUtil.selectVideoEncoder()!!,
                        MediaFormat.MIMETYPE_VIDEO_AVC
                    )
                )
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val videoEncoder = MediaCodec.createByCodecName(videoEncoderName)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            // Audio encoder (if available).
            var audioEncoderCodec: MediaCodec? = null
            if (hasAac) {
                val audioFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC, 48_000,
                    /* channelCount= */ 1
                ).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                }
                audioEncoderCodec = MediaCodec.createByCodecName(audioEncoder!!)
                audioEncoderCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                audioEncoderCodec.start()
            }

            // ------------------------------------------------------------------
            // Render loop — drives AnimationSystem + GLES2 into MediaCodec Surface
            // ------------------------------------------------------------------
            val egl = EglRenderTarget(inputSurface, evenWidth, evenHeight)
            egl.setup()
            progress(0.05f, "Encoder ready")

            val totalDurationMs = (framesSource.totalSamples() * 1000L) / 48_000L
            val expectedFrames = (totalDurationMs * fps) / 1000L
            var frameIndex = 0
            val frameDurationMs = 1000 / fps

            while (!framesSource.isExhausted()) {
                val audioChunk = FloatArray(48_000 * frameDurationMs / 1000)
                val framesRead = framesSource.renderChunk(audioChunk)
                if (framesRead > 0) {
                    if (audioEncoderCodec != null) {
                        feedAudioEncoder(audioEncoderCodec, audioChunk, framesRead)
                    }
                    // Advance the simulation with the audio frames we consumed.
                    animationSystem.advance(framesRead, 48_000)
                }

                // Pull the latest rendered state and draw it into the EGL surface.
                val state: SceneRenderState? = animationSystem.latestRenderState()
                if (state != null) {
                    egl.drawSceneFrame(state)
                }

                // Drain encoder output periodically.
                drainEncoder(
                    videoEncoder, muxer, isVideo = true,
                    trackIndexRef = { videoTrackIndex }, trackIndexSetter = { videoTrackIndex = it },
                    muxerStartedRef = { muxerStarted }, muxerStartedSetter = { muxerStarted = it }
                )
                if (audioEncoderCodec != null) {
                    drainEncoder(
                        audioEncoderCodec, muxer, isVideo = false,
                        trackIndexRef = { audioTrackIndex }, trackIndexSetter = { audioTrackIndex = it },
                        muxerStartedRef = { muxerStarted }, muxerStartedSetter = { muxerStarted = it }
                    )
                }

                frameIndex += 1
                if (frameIndex % 4 == 0) {
                    val p = if (expectedFrames > 0) {
                        frameIndex.toFloat() / expectedFrames.toFloat()
                    } else 0f
                    progress(p.coerceIn(0f, 1f), "Encoded $frameIndex frames")
                    yield()
                }
            }

            // Signal EOS.
            videoEncoder.signalEndOfInputStream()
            drainEncoder(
                videoEncoder, muxer, isVideo = true,
                trackIndexRef = { videoTrackIndex }, trackIndexSetter = { videoTrackIndex = it },
                muxerStartedRef = { muxerStarted }, muxerStartedSetter = { muxerStarted = it },
                eos = true
            )
            audioEncoderCodec?.let { codec ->
                codec.signalEndOfInputStream()
                drainEncoder(
                    codec, muxer, isVideo = false,
                    trackIndexRef = { audioTrackIndex }, trackIndexSetter = { audioTrackIndex = it },
                    muxerStartedRef = { muxerStarted }, muxerStartedSetter = { muxerStarted = it },
                    eos = true
                )
            }
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            videoEncoder.release()
            audioEncoderCodec?.release()
            egl.release()
            progress(1.0f, "Export complete")
        } finally {
            // Best-effort cleanup if the try block failed before release.
            try {
                if (muxerStarted) muxer.stop()
                muxer.release()
            } catch (_: Exception) { }
        }
    }

    private fun feedAudioEncoder(codec: MediaCodec, samples: FloatArray, frames: Int) {
        val inIdx = codec.dequeueInputBuffer(10_000)
        if (inIdx >= 0) {
            val inBuf = codec.getInputBuffer(inIdx) ?: return
            inBuf.clear()
            // Convert float32 → int16 little-endian.
            val bytes = ByteArray(frames * 2)
            for (i in 0 until frames) {
                val clamped = samples[i].coerceIn(-1f, 1f)
                val s = (clamped * 32_767f).toInt()
                bytes[i * 2] = (s and 0xFF).toByte()
                bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
            }
            inBuf.put(bytes)
            codec.queueInputBuffer(inIdx, 0, bytes.size, frames * 1_000_000 / 48_000, 0)
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        isVideo: Boolean,
        trackIndexRef: () -> Int,
        trackIndexSetter: (Int) -> Unit,
        muxerStartedRef: () -> Boolean,
        muxerStartedSetter: (Boolean) -> Unit,
        eos: Boolean = false
    ) {
        val info = MediaCodec.BufferInfo()
        val timeoutUs = if (eos) 10_000 else 0L
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, timeoutUs)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!eos) return else continue
                }
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    val trackIdx = muxer.addTrack(newFormat)
                    trackIndexSetter(trackIdx)
                    if (!muxerStartedRef()) {
                        muxer.start()
                        muxerStartedSetter(true)
                    }
                }
                idx >= 0 -> {
                    val buf: ByteBuffer = codec.getOutputBuffer(idx) ?: return
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        info.size = 0
                    }
                    if (info.size > 0 && muxerStartedRef()) {
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val targetTrack = trackIndexRef()
                        if (targetTrack >= 0) {
                            muxer.writeSampleData(targetTrack, buf, info)
                        }
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    private fun computeBitrate(width: Int, height: Int): Int {
        // 0.10 bits per pixel × fps is a reasonable baseline for
        // monochrome pixel-art. The encoder may exceed or undershoot
        // but this gives the rate-control a sane default.
        return ((width * height * fps) / 10).coerceAtLeast(400_000)
    }
}

/**
 * Minimal EGL14 + GLES2 surface target for monochrome 1-bit raster
 * rendering from a [SceneRenderState] into a MediaCodec input Surface.
 *
 * Pipeline per frame:
 *   1. Unpack the 1-bit MSB-first [SceneRenderState.pixels] into an
 *      8-bit luminance buffer.
 *   2. Upload to a GL_LUMINANCE texture (GLES 2.0 compatible, no
 *      GL_RED requirement).
 *   3. Draw a full-screen quad sampling that texture.
 *   4. eglSwapBuffers to deliver the frame to MediaCodec.
 */
class EglRenderTarget(
    private val surface: android.view.Surface,
    private val width: Int,
    private val height: Int
) {
    private var eglDisplay: android.opengl.EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: android.opengl.EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: android.opengl.EGLSurface = EGL14.EGL_NO_SURFACE
    private var program: Int = 0
    private var vertexBuffer: Int = 0
    private var texture: Int = 0

    fun setup() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(eglDisplay, intArrayOf(0, 0), 0, intArrayOf(0, 0), 0)

        // Single valid eglChooseConfig call.
        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numConfigs = intArrayOf(0)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        val config = configs[0]

        val ctxAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, config,
            EGL14.EGL_NO_CONTEXT, ctxAttribs, 0
        )

        val surfAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

        program = compileProgram()

        // Full-screen quad vertex buffer (two triangles covering [-1,1]×[-1,1]).
        val vertices = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val vbb = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
        val fbb = vbb.asFloatBuffer().apply { put(vertices); position(0) }

        val buffers = intArrayOf(0)
        GLES20.glGenBuffers(1, buffers, 0)
        vertexBuffer = buffers[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertices.size * 4, fbb,
            GLES20.GL_STATIC_DRAW
        )

        // Texture object for the 1-bit → luminance upload.
        val textures = intArrayOf(0)
        GLES20.glGenTextures(1, textures, 0)
        texture = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun drawSceneFrame(state: SceneRenderState) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (state.pixels.isNotEmpty()) {
            GLES20.glUseProgram(program)

            // Unpack 1-bit MSB-first pixels → 8-bit luminance buffer.
            val unpackData = ByteBuffer.allocateDirect(state.widthPx * state.heightPx)
            for (y in 0 until state.heightPx) {
                for (x in 0 until state.widthPx) {
                    val byte = state.pixels[y * state.rowBytes + (x ushr 3)].toInt()
                    val bit = (byte ushr (7 - (x and 7))) and 1
                    unpackData.put((if (bit == 1) 0xFF.toByte() else 0x00.toByte()))
                }
            }
            unpackData.position(0)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                state.widthPx, state.heightPx, 0, GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE, unpackData
            )

            val texLoc = GLES20.glGetUniformLocation(program, "u_tex")
            GLES20.glUniform1i(texLoc, 0)

            val posLoc = GLES20.glGetAttribLocation(program, "a_pos")
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer)
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(posLoc)
        }

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun compileProgram(): Int {
        // Y-inverted UV so top-down pixel streams render upright.
        val vs = """
            attribute vec2 a_pos;
            varying vec2 v_uv;
            void main() {
                v_uv = vec2(a_pos.x * 0.5 + 0.5, 0.5 - a_pos.y * 0.5);
                gl_Position = vec4(a_pos, 0.0, 1.0);
            }
        """.trimIndent()
        val fs = """
            precision mediump float;
            uniform sampler2D u_tex;
            varying vec2 v_uv;
            void main() {
                gl_FragColor = vec4(texture2D(u_tex, v_uv).rrr, 1.0);
            }
        """.trimIndent()
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        return p
    }

    private fun compileShader(type: Int, source: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, source)
        GLES20.glCompileShader(s)
        return s
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }
    }
}
