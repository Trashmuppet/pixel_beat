# ADR-005: Offline Renderer Is The Reference

| Field | Value |
|---|---|
| **Date** | 2026-07-25 |
| **Status** | Accepted |
| **Deciders** | Engineering |
| **Consulted** | Release engineering |

## Context

[`12_EXPORT_PIPELINE.md`](../12_EXPORT_PIPELINE.md) demands
"identical input ⇒ identical output." We must be honest about
what that's measurable against: byte-identical WAV and GIF are
fully achievable; MP4 on hardware H.264 encoders is not.

## Decision

| Format | Determinism class |
|---|---|
| `.mbeat` (UTF-8 JSON via kotlinx.serialization) | byte-identical |
| WAV (16-bit PCM mono 48 kHz) | byte-identical |
| GIF (1-bit indexed LZW) | byte-identical |
| MP4 (H.264 + AAC) | **visually-identical** only |
| SceneRenderState (`pixels: ByteArray`) | byte-identical |

* WAV: SHA-256 of the file equals across runs given identical
  input. `ExportDeterminismTest` enforces this.
* GIF: `GifEncoder` clears the LZW dictionary **once**, on the
  first frame, and never resets; maximum code size 12. Byte-equal
  output is a hard invariant.
* MP4: per-SoC H.264 bitstream variance is a known hardware
  constraint; we encode through the platform's best H.264
  encoder. The test asserts per-frame grayscale MD5 + frame
  count — visually identical across runs. A future Phase 6+
  follow-up can pin a software encoder (e.g. x264) per FBH build
  target to reach byte-identical; we accept the SoC variance for V1.
* AAC fallback: if the platform lacks an AAC encoder, `Mp4MediaCodecEncoder`
  falls back to a PCM-only MP4 + emits an ADR-005 warning. The user
  sees the same file path with the same extension; the data
  layout (PCM-in-MP4) is valid ISO/IEC 14496-12.

## Consequences

* Positive: WAV / GIF golden-fixture tests are stable on every
  device we ship to. CPU + Play Store upload can rely on them.
* Positive: SceneRenderState determinism tests (Phase 4
  SceneRuntimeTest) are stable, allowing Compose preview / export
  tests to align.
* Negative: MP4 export is *visually* deterministic — re-rendering
  on a different phone gives an identical-looking video but a
  byte-different file. We document this in release notes.
* Out of scope: DRM-protected MP4 export. The product is offline;
  no DRM/watermark surface is needed.
