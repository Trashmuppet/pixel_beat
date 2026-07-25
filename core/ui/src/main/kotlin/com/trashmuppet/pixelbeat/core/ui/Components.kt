package com.trashmuppet.pixelbeat.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trashmuppet.pixelbeat.core.model.DrumKind
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.SwingMode

// ---------------------------------------------------------------------------
// Sequencer components
// ---------------------------------------------------------------------------

/**
 * One step in the 16-step grid. Inverted (foreground fill) when
 * active. Tappable with haptic confirmation.
 *
 * When [isPlayheadStep] is true the cell additionally inverts — the
 * fill XORs against [active] so the playhead pulses a visual
 * "passing through" affordance in 1-bit (ADR-001 + `16_UI_BIBLE.md`
 * §Playing state). The tick position itself is still owned by the
 * transport; this only paints the visual marker.
 *
 * Tap target >= 48dp per `16_UI_BIBLE.md`. Colours are mono.
 */
@Composable
fun StepCell(
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Dp = 48.dp,
    isPlayheadStep: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val fg = MonoPalette.Foreground
    val bg = MonoPalette.Background
    // 1-bit semantics:
    //   active || at playhead  -> solid FG fill (cell is lit)
    //   neither                -> BG fill, FG border (cell is empty)
    // The playhead additionally bumps the border from 1.dp to 2.dp so
    // the cursor line is visible regardless of cell activity — both
    // signals preserved without the XOR "active+delayed-flip" surprise.
    val fill = if (active || isPlayheadStep) fg else bg
    val borderWidth = if (isPlayheadStep) 2.dp else 1.dp
    Box(
        modifier = modifier
            .size(cellSize)
            .clip(RoundedCornerShape(4.dp))
            .border(width = borderWidth, color = fg)
            .background(fill)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .semantics {
                contentDescription = when {
                    active && isPlayheadStep -> "Active step, playing now"
                    isPlayheadStep -> "Playhead position, inactive"
                    active -> "Active step"
                    else -> "Inactive step"
                }
                role = Role.Checkbox
                stateDescription = if (isPlayheadStep) "playing" else "idle"
            }
    )
}

/**
 * Track header — instrument id + drum kind label + mute toggle.
 */
@Composable
fun TrackHeader(
    name: String,
    drumKind: DrumKind,
    muted: Boolean,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .height(TapTarget)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = name, color = MonoPalette.Foreground, fontSize = 14.sp)
        Text(text = drumKind.name.lowercase(), color = MonoPalette.Foreground, fontSize = 12.sp)
        Spacer(modifier = Modifier.sizeIn(minWidth = 16.dp))
        Text(
            text = if (muted) "M" else "U",
            color = MonoPalette.Foreground,
            fontSize = 14.sp,
            modifier = Modifier
                .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMuteToggle()
                }
                .semantics {
                    contentDescription = if (muted) "Unmute track" else "Mute track"
                    role = Role.Switch
                }
        )
    }
}

/**
 * Pattern selector — chip row over the available patterns.
 */
@Composable
fun PatternSelector(
    patterns: List<Pattern>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        patterns.forEachIndexed { idx, pattern ->
            val isCurrent = idx == currentIndex
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(idx)
                },
                modifier = Modifier
                    .sizeIn(minHeight = TapTarget)
                    .semantics { contentDescription = "Pattern ${pattern.id}" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrent) MonoPalette.Foreground else MonoPalette.Background,
                    contentColor = if (isCurrent) MonoPalette.Background else MonoPalette.Foreground
                )
            ) { Text(pattern.id) }
        }
    }
}

/**
 * Discrete-step playhead — back-compatible entrypoint for tests and
 * call sites that don't have a live transport sample. Live UI uses
 * [AnimatedPlayhead] instead; see ADR-001.
 */
@Composable
fun Playhead(
    stepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    require(totalSteps > 0) { "totalSteps must be > 0" }
    val safeIndex = ((stepIndex % totalSteps) + totalSteps) % totalSteps
    val widthPerStep = if (totalSteps > 0) 1f / totalSteps else 1f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MonoPalette.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthPerStep)
                .height(4.dp)
                .background(MonoPalette.Foreground)
                .padding(start = (safeIndex * widthPerStep.toFloat().coerceAtLeast(1f)).dp)
        )
    }
}

/**
 * Pure-math contract used by [AnimatedPlayhead].
 *
 * Kept as an `internal object` so the unit test in `core/ui/src/test`
 * exercises the wire-level math without needing a Compose runtime.
 *
 * Cited contracts:
 *  - `09_SCENE_SYSTEM.md` — Scene simulation is fixed 240 Hz.
 *  - ADR-004 — `Scene.step()` MUST be pure; the playhead UI MAY
 *    interpolate visually but the tick position is owned by the
 *    transport (ADR-001).
 *
 * @see AnimatedPlayhead
 */
internal object PlayheadMath {
    /**
     * Number of audio samples in one 16th note at the given BPM.
     *
     * `sixteenthSeconds = 60 / bpm / 4` ⇒ `sixteenthSamples = sampleRate * sixteenthSeconds`.
     */
    fun sixteenthSampleCount(sampleRate: Int, bpm: Float): Long {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(bpm > 0f) { "bpm must be positive" }
        val sixteenthSeconds = 60.0 / bpm.toDouble() / 4.0
        return (sampleRate.toDouble() * sixteenthSeconds).toLong().coerceAtLeast(1L)
    }

    /**
     * Map an elapsed sample position to a fractional playhead position
     * in `[0f, 1f)` across the current [totalSteps] bar. Wraps modulo
     * `totalSteps` so the playhead visually loops with the project.
     *
     * `stepFloat = stepIndex + (sampleInSixteenth / sixteenthSamples)`.
     * Result clamped to `[0f, 1f)` so sub-pixel overflow can't leak.
     */
    fun sampleToStepFraction(sample: Long, sixteenthSamples: Long, totalSteps: Int): Float {
        require(sixteenthSamples > 0) { "sixteenthSamples must be positive" }
        if (totalSteps <= 0) return 0f
        val safeSample = sample.coerceAtLeast(0L)
        val stepIndex = ((safeSample / sixteenthSamples) % totalSteps).toInt()
        val fracInStep = ((safeSample % sixteenthSamples).toFloat() / sixteenthSamples.toFloat())
            .coerceIn(0f, 1f)
        val stepFloat = stepIndex + fracInStep
        return (stepFloat / totalSteps.toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * Animated playhead bar.
 *
 * The transport ([RealtimeTransport]) emits sample-accurate positions
 * via `positionFlow()`. UI must not own the tick counter (ADR-001).
 * To avoid jitter between emissions (audio buffers fire every few
 * ms; Compose frames every ~16 ms), we render the bar at a
 * sub-step fractional position derived from the **last transport
 * emission timestamp + the current `withFrameNanos` delta**.
 *
 * `09_SCENE_SYSTEM.md` and ADR-004 codify that the *tick position*
 * stays on the transport — this component only smooths the *visual*
 * position. The `stepIndex` math runs in [PlayheadMath] so it's test
 * independent of Compose.
 *
 * @param samplePosition  Latest transport sample position (Long).
 * @param sampleRate      Audio sample rate (typically 48 kHz).
 * @param bpm             Current project BPM (used for sixteenth math).
 * @param totalSteps      Pattern length step count (e.g. 16).
 * @param modifier        Layout modifier from the parent.
 * @param barHeightDp     Marker bar height.
 * @param markerWidthDp   Marker bar width.
 */
@Composable
fun AnimatedPlayhead(
    samplePosition: Long,
    sampleRate: Int,
    bpm: Float,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    barHeightDp: Dp = 6.dp,
    markerWidthDp: Dp = 4.dp
) {
    // Defensive: render an empty background while inputs are invalid
    // so the parent layout doesn't collapse mid-flight.
    if (totalSteps <= 0 || sampleRate <= 0 || bpm <= 0f) {
        Box(modifier.fillMaxWidth().height(barHeightDp).background(MonoPalette.Background))
        return
    }

    val sixteenthSamples = remember(sampleRate, bpm) {
        PlayheadMath.sixteenthSampleCount(sampleRate, bpm)
    }

    // Last transport emission: anchor for frame-time interpolation.
    val lastEmitNanos = remember { mutableLongStateOf(0L) }
    val lastEmitSample = remember(samplePosition) { mutableLongStateOf(samplePosition) }

    LaunchedEffect(samplePosition) {
        // When samplePosition changes, capture the frame nanos at
        // the moment we observed it. The next animation frame will
        // project forward from this anchor.
        withFrameNanos { now -> lastEmitNanos.longValue = now }
        lastEmitSample.longValue = samplePosition
    }

    // Continuous frame-locked smoothing loop. ADR-001 explicitly
    // permits withFrameNanos for visual smoothing — only the tick
    // position is owned by the transport.
    val frameSample = remember { mutableLongStateOf(samplePosition.coerceAtLeast(0L)) }
    LaunchedEffect(sampleRate, sixteenthSamples) {
        while (true) {
            val now = withFrameNanos { it }
            val elapsedNanos = (now - lastEmitNanos.longValue).coerceAtLeast(0L)
            val projected = lastEmitSample.longValue +
                (elapsedNanos.toDouble() * sampleRate.toDouble() / 1_000_000_000.0).toLong()
            frameSample.longValue = projected.coerceAtLeast(0L)
        }
    }

    val fraction = PlayheadMath.sampleToStepFraction(
        sample = frameSample.longValue,
        sixteenthSamples = sixteenthSamples,
        totalSteps = totalSteps
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightDp)
            .background(MonoPalette.Background)
            .semantics { contentDescription = "Playhead at fraction ${(fraction * 100f).toInt()} percent" },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val safeMarkerWidth = minOf(markerWidthDp.toPx(), canvasWidth)
            val x = (fraction * (canvasWidth - safeMarkerWidth)).coerceIn(0f, canvasWidth - safeMarkerWidth)
            drawRect(
                color = MonoPalette.Foreground,
                topLeft = Offset(x, 0f),
                size = Size(safeMarkerWidth, size.height)
            )
        }
    }
}

/**
 * BPM stepper — slider 30..300 with a readout.
 */
@Composable
fun TempoControl(
    bpm: Float,
    onBpmChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    min: Float = 30f,
    max: Float = 300f
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Tempo: ${bpm.toInt()} bpm", color = MonoPalette.Foreground)
        Slider(
            value = bpm.coerceIn(min, max),
            onValueChange = { onBpmChange(it) },
            onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = MonoPalette.Foreground,
                activeTrackColor = MonoPalette.Foreground,
                inactiveTrackColor = MonoPalette.Background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = TapTarget)
                .semantics {
                    contentDescription = "Tempo in beats per minute"
                    stateDescription = "${bpm.toInt()} bpm"
                }
        )
    }
}

/**
 * Swing control — granularity chip row + amount slider.
 */
@Composable
fun SwingControl(
    swing: SwingMode,
    onSwingChange: (SwingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SwingMode.Granularity.entries.forEach { granularity ->
                val selected = swing.granularity == granularity
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwingChange(swing.copy(granularity = granularity))
                    },
                    modifier = Modifier
                        .sizeIn(minHeight = TapTarget)
                        .semantics { contentDescription = "Swing granularity ${granularity.name}" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MonoPalette.Foreground else MonoPalette.Background,
                        contentColor = if (selected) MonoPalette.Background else MonoPalette.Foreground
                    )
                ) { Text(granularity.name.lowercase()) }
            }
        }
        Slider(
            value = swing.amount.coerceIn(0f, 1f),
            onValueChange = { onSwingChange(swing.copy(amount = it)) },
            onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            colors = SliderDefaults.colors(
                thumbColor = MonoPalette.Foreground,
                activeTrackColor = MonoPalette.Foreground,
                inactiveTrackColor = MonoPalette.Background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = TapTarget)
                .semantics {
                    contentDescription = "Swing amount"
                    stateDescription = "Swing ${(swing.amount * 100).toInt()} percent"
                }
        )
        Text(
            "Amount: ${(swing.amount * 100).toInt()}%",
            color = MonoPalette.Foreground,
            fontSize = 12.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Project + export components
// ---------------------------------------------------------------------------

/**
 * Recent-project card. Tap to open; long press reserved for future
 * delete / rename affordance (Phase 3 wires only the tap).
 */
@Composable
fun ProjectCard(
    project: MBeatProject,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = TapTarget)
            .border(width = 1.dp, color = MonoPalette.Foreground)
            .background(MonoPalette.Background)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(16.dp)
            .semantics { contentDescription = "Open project ${project.name}" }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(project.name, color = MonoPalette.Foreground)
            Text(
                "${project.bpm.toInt()} bpm · ${project.patterns.size} patterns",
                color = MonoPalette.Foreground,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Lazy list of recent projects using `ProjectCard`.
 */
@Composable
fun RecentProjectList(
    projects: List<MBeatProject>,
    onSelect: (MBeatProject) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(items = projects, key = { it.id }) { project ->
            ProjectCard(project = project, onClick = { onSelect(project) })
        }
    }
}

/**
 * Format selector for the export screen — chip row of `ExportFormat`.
 *
 * The format enum lives in `:feature-export`, so callers supply the
 * list from there. The component itself stays UI-only.
 */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    contentDesc: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelect(option)
                },
                modifier = Modifier
                    .sizeIn(minHeight = TapTarget)
                    .semantics { contentDescription = contentDesc(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MonoPalette.Foreground else MonoPalette.Background,
                    contentColor = if (isSelected) MonoPalette.Background else MonoPalette.Foreground
                )
            ) { Text(label(option)) }
        }
    }
}

/**
 * Progress indicator for the export screen.
 */
@Composable
fun ExportProgressBar(
    progress: Float,
    statusLine: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .semantics { contentDescription = "Export progress ${(progress * 100).toInt()} percent" },
            color = MonoPalette.Foreground,
            trackColor = MonoPalette.Background
        )
        Text(statusLine, color = MonoPalette.Foreground, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// Pro gating
// ---------------------------------------------------------------------------

/**
 * Wraps [content] in a Pro entitlement gate.
 *
 * When [requiresPro] and the user is not Pro: renders [content] dimmed
 * with a lock overlay and an "Unlock Pro" button. Otherwise renders
 * [content] without modification.
 */
@Composable
fun ProGate(
    requiresPro: Boolean,
    isPro: Boolean,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (requiresPro && !isPro) {
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = modifier.semantics(mergeDescendants = true) {
                contentDescription = "Pro Required"
            }
        ) {
            Box(modifier = Modifier.alpha(0.3f)) {
                content()
            }
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pro Required", color = MonoPalette.Foreground, fontSize = 14.sp)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUnlockClick()
                        },
                        modifier = Modifier.sizeIn(minHeight = TapTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MonoPalette.Foreground,
                            contentColor = MonoPalette.Background
                        )
                    ) {
                        Text("Unlock Pro")
                    }
                }
            }
        }
    } else {
        content()
    }
}
