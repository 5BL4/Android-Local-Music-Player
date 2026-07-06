package com.musicplayer.localmusicplayer.presentation.player.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.domain.model.WaveformStyle
import kotlin.math.sin

// Visual constants — tuned to feel at home next to the album art and playback
// controls. Kept private so the only public surface is the composable itself.
private val WaveformHeight: Dp = 72.dp
private val BarWidth: Dp = 3.dp
private val BarSpacing: Dp = 2.dp
private val HorizontalInset: Dp = 16.dp
private val VerticalInset: Dp = 6.dp
private val MinBarHeight: Dp = 2.dp
private val LineStrokeWidth: Dp = 2.5.dp
private val FlatlineAlpha: Float = 0.25f

/**
 * Real-time audio waveform visualization for the now-playing screen.
 *
 * Renders a rolling window of normalized amplitudes (0..1) in one of three visual
 * styles. The component is a single `Canvas` — all drawing happens in the draw
 * phase, so updating [amplitudes] never recomposes child layout, it just
 * repaints.
 *
 * The component intentionally ships a transparent background: on the player
 * screen it sits between the controls and any trailing content, and a separate
 * `Surface` container tends to fight the album-art card above it. Callers that
 * want a card can wrap this composable in a `Surface` themselves.
 *
 * @param amplitudes Normalized amplitudes in 0..1, ordered **oldest -> newest**.
 *   Only the most recent samples that fit the visible area are drawn. Pass an
 *   empty list to render the idle/flatline state.
 * @param style Visual style of the waveform.
 * @param modifier Modifier applied to the underlying [Canvas].
 * @param barColor Color used to draw the bars or line. Defaults to the theme
 *   primary so it picks up the user's chosen accent color.
 */
@Composable
fun WaveformVisualization(
    amplitudes: List<Float>,
    style: WaveformStyle,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(WaveformHeight)
    ) {
        val centerY = size.height / 2f

        val barWidthPx = BarWidth.toPx()
        val spacingPx = BarSpacing.toPx()
        val pitch = barWidthPx + spacingPx
        val insetPx = HorizontalInset.toPx()
        val drawableWidth = (size.width - insetPx * 2f).coerceAtLeast(pitch)
        val visibleCount = (drawableWidth / pitch).toInt().coerceAtLeast(1)

        // Rolling window: drop the oldest samples once we exceed what fits.
        val displayed = if (amplitudes.size > visibleCount) {
            amplitudes.takeLast(visibleCount)
        } else {
            amplitudes
        }

        if (displayed.isEmpty()) {
            drawFlatline(insetPx, centerY, size.width, barColor)
            return@Canvas
        }

        when (style) {
            WaveformStyle.MirroredBars -> drawMirroredBars(
                displayed = displayed,
                barColor = barColor,
                centerY = centerY,
                halfHeight = size.height / 2f,
                barWidthPx = barWidthPx,
                insetPx = insetPx,
                pitch = pitch,
            )
            WaveformStyle.Bars -> drawSingleBars(
                displayed = displayed,
                barColor = barColor,
                centerY = centerY,
                halfHeight = size.height / 2f,
                barWidthPx = barWidthPx,
                insetPx = insetPx,
                pitch = pitch,
            )
            WaveformStyle.Line -> drawWaveformLine(
                displayed = displayed,
                barColor = barColor,
                centerY = centerY,
                halfHeight = size.height / 2f,
                insetPx = insetPx,
                pitch = pitch,
            )
        }
    }
}

// --- Renderers ---------------------------------------------------------------

/**
 * MirroredBars — symmetric top + bottom reflection around the horizontal axis.
 * Reads as an oscilloscope / heartbeat. The most energetic of the three styles
 * because the full vertical extent is always in use.
 */
private fun DrawScope.drawMirroredBars(
    displayed: List<Float>,
    barColor: Color,
    centerY: Float,
    halfHeight: Float,
    barWidthPx: Float,
    insetPx: Float,
    pitch: Float,
) {
    val maxHalfBar = halfHeight - VerticalInset.toPx()
    val minHalfBar = MinBarHeight.toPx() / 2f
    val cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

    displayed.forEachIndexed { i, amp ->
        val halfH = (amp.coerceIn(0f, 1f) * maxHalfBar).coerceAtLeast(minHalfBar)
        val x = insetPx + i * pitch
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, centerY - halfH),
            size = Size(barWidthPx, halfH * 2f),
            cornerRadius = cornerRadius,
        )
    }
}

/**
 * Bars — single-direction bars rising from the center axis. Reads as a
 * precision audio level meter: the empty lower half makes the rhythm of peaks
 * and silences more legible than the mirrored variant.
 */
private fun DrawScope.drawSingleBars(
    displayed: List<Float>,
    barColor: Color,
    centerY: Float,
    halfHeight: Float,
    barWidthPx: Float,
    insetPx: Float,
    pitch: Float,
) {
    val maxBar = halfHeight - VerticalInset.toPx()
    val minBar = MinBarHeight.toPx()
    val cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

    displayed.forEachIndexed { i, amp ->
        val barH = (amp.coerceIn(0f, 1f) * maxBar).coerceAtLeast(minBar)
        val x = insetPx + i * pitch
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, centerY - barH),
            size = Size(barWidthPx, barH),
            cornerRadius = cornerRadius,
        )
    }
}

/**
 * Line — a continuous polyline through the amplitude samples, centered on the
 * same axis as the bar styles. The round stroke cap softens endpoints and
 * joints, giving a flowing envelope feel without explicit bezier smoothing.
 */
private fun DrawScope.drawWaveformLine(
    displayed: List<Float>,
    barColor: Color,
    centerY: Float,
    halfHeight: Float,
    insetPx: Float,
    pitch: Float,
) {
    if (displayed.isEmpty()) return
    val maxAmplitude = halfHeight - VerticalInset.toPx()
    val strokeWidth = LineStrokeWidth.toPx()

    val path = Path()
    displayed.forEachIndexed { i, amp ->
        val y = centerY - amp.coerceIn(0f, 1f) * maxAmplitude
        val x = insetPx + i * pitch
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = barColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

/**
 * Flatline — a thin, low-alpha line across the center axis. Keeps the
 * component's footprint occupied when no audio is playing or before the first
 * amplitude tick arrives, instead of flashing to a blank rectangle.
 */
private fun DrawScope.drawFlatline(
    insetPx: Float,
    centerY: Float,
    canvasWidth: Float,
    barColor: Color,
) {
    drawLine(
        color = barColor.copy(alpha = FlatlineAlpha),
        start = Offset(insetPx, centerY),
        end = Offset(canvasWidth - insetPx, centerY),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

// --- Previews ----------------------------------------------------------------

/**
 * Deterministic 64-sample waveform that looks like a real audio envelope:
 * a slow carrier modulated by faster detail, clamped to a small floor so even
 * the "silent" stretches have presence in the preview.
 */
private fun sampleAmplitudes(): List<Float> = (0..63).map { i ->
    val slow = sin(i * 0.18) * 0.42
    val fast = sin(i * 0.71) * 0.22
    val combined = slow + fast + 0.46
    combined.toFloat().coerceIn(0.05f, 1f)
}

@Preview(name = "Mirrored Bars", showBackground = true)
@Preview(name = "Mirrored Bars (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaveformVisualizationMirroredPreview() {
    MaterialTheme {
        WaveformVisualization(
            amplitudes = sampleAmplitudes(),
            style = WaveformStyle.MirroredBars,
        )
    }
}

@Preview(name = "Bars", showBackground = true)
@Preview(name = "Bars (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaveformVisualizationBarsPreview() {
    MaterialTheme {
        WaveformVisualization(
            amplitudes = sampleAmplitudes(),
            style = WaveformStyle.Bars,
        )
    }
}

@Preview(name = "Smooth Line", showBackground = true)
@Preview(name = "Smooth Line (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaveformVisualizationLinePreview() {
    MaterialTheme {
        WaveformVisualization(
            amplitudes = sampleAmplitudes(),
            style = WaveformStyle.Line,
        )
    }
}

@Preview(name = "Flatline / Idle", showBackground = true)
@Preview(name = "Flatline / Idle (dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaveformVisualizationFlatlinePreview() {
    MaterialTheme {
        WaveformVisualization(
            amplitudes = emptyList(),
            style = WaveformStyle.MirroredBars,
        )
    }
}
