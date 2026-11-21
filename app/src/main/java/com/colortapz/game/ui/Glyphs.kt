package com.colortapz.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Chrome icons drawn with Canvas instead of emoji.
 *
 * Emoji glyphs (🔊 📳 🏆 🎨) render differently on every OEM and Android
 * version, so the UI - and every store screenshot - changed shape depending on
 * the device. These are deterministic and tint with the theme.
 */

@Composable
fun SoundGlyph(enabled: Boolean, tint: Color = Color.White, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawSpeakerBody(tint)
        if (enabled) {
            drawWave(tint, scale = 0.55f)
            drawWave(tint, scale = 0.85f)
        } else {
            drawCross(tint, startX = 0.60f, endX = 0.94f)
        }
    }
}

@Composable
fun HapticsGlyph(enabled: Boolean, tint: Color = Color.White, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Phone body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.20f),
            size = Size(w * 0.28f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.06f)
        )
        if (enabled) {
            drawVibrationArc(tint, side = -1)
            drawVibrationArc(tint, side = 1)
        } else {
            drawCross(tint, startX = 0.68f, endX = 0.96f)
        }
    }
}

@Composable
fun PauseGlyph(tint: Color = Color.White, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barWidth = w * 0.18f
        val radius = CornerRadius(barWidth * 0.4f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.22f),
            size = Size(barWidth, h * 0.56f),
            cornerRadius = radius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.52f, h * 0.22f),
            size = Size(barWidth, h * 0.56f),
            cornerRadius = radius
        )
    }
}

@Composable
fun CloseGlyph(tint: Color = Color.White, size: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawCross(tint, startX = 0.22f, endX = 0.78f)
    }
}

@Composable
fun RanksGlyph(tint: Color = Color.White, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barWidth = w * 0.20f
        val gap = w * 0.10f
        val heights = listOf(0.40f, 0.66f, 0.52f)
        heights.forEachIndexed { index, fraction ->
            val barHeight = h * fraction
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.12f + index * (barWidth + gap), h - barHeight - h * 0.08f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.35f)
            )
        }
    }
}

@Composable
fun BadgeGlyph(tint: Color = Color.White, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawPath(starPath(this, points = 5), tint)
    }
}

@Composable
fun PaletteGlyph(tint: Color = Color.White, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            color = tint,
            radius = w * 0.34f,
            center = Offset(w * 0.46f, h * 0.52f),
            style = Stroke(width = w * 0.10f)
        )
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.34f, h * 0.40f))
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.44f, h * 0.68f))
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.62f, h * 0.46f))
    }
}

@Composable
fun LockGlyph(tint: Color = Color.White, size: Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.26f, h * 0.46f),
            size = Size(w * 0.48f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.08f)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.32f, h * 0.16f),
            size = Size(w * 0.36f, h * 0.42f),
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HeartGlyph(filled: Boolean, tint: Color, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val path = heartPath(this)
        if (filled) {
            drawPath(path, tint)
        } else {
            drawPath(path, tint, style = Stroke(width = this.size.width * 0.10f))
        }
    }
}

@Composable
fun CheckGlyph(tint: Color, size: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.80f, h * 0.28f)
        }
        drawPath(path, tint, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round))
    }
}

// --- drawing helpers -------------------------------------------------------

private fun DrawScope.drawSpeakerBody(tint: Color) {
    val w = size.width
    val h = size.height
    val bodyWidth = w * 0.20f
    val bodyHeight = h * 0.32f
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.06f, h / 2f - bodyHeight / 2f),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(w * 0.03f)
    )
    val cone = Path().apply {
        moveTo(w * 0.26f, h * 0.34f)
        lineTo(w * 0.50f, h * 0.14f)
        lineTo(w * 0.50f, h * 0.86f)
        lineTo(w * 0.26f, h * 0.66f)
        close()
    }
    drawPath(cone, tint)
}

private fun DrawScope.drawWave(tint: Color, scale: Float) {
    val w = size.width
    val h = size.height
    val arcWidth = w * 0.34f * scale
    drawArc(
        color = tint,
        startAngle = -55f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(w * 0.52f, h / 2f - (h * 0.44f * scale) / 2f),
        size = Size(arcWidth, h * 0.44f * scale),
        style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawVibrationArc(tint: Color, side: Int) {
    val w = size.width
    val h = size.height
    val startX = if (side < 0) w * 0.10f else w * 0.62f
    drawArc(
        color = tint,
        startAngle = if (side < 0) 110f else -70f,
        sweepAngle = if (side < 0) 140f else -140f,
        useCenter = false,
        topLeft = Offset(startX, h * 0.28f),
        size = Size(w * 0.28f, h * 0.44f),
        style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCross(tint: Color, startX: Float, endX: Float) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.09f
    drawLine(
        color = tint,
        start = Offset(w * startX, h * 0.26f),
        end = Offset(w * endX, h * 0.74f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = tint,
        start = Offset(w * endX, h * 0.26f),
        end = Offset(w * startX, h * 0.74f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun starPath(scope: DrawScope, points: Int): Path {
    val w = scope.size.width
    val h = scope.size.height
    val center = Offset(w / 2f, h / 2f)
    val outer = minOf(w, h) * 0.46f
    val inner = outer * 0.44f
    val path = Path()
    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outer else inner
        val angle = (-PI / 2) + (index * PI / points)
        val x = center.x + (cos(angle) * radius).toFloat()
        val y = center.y + (sin(angle) * radius).toFloat()
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun heartPath(scope: DrawScope): Path {
    val w = scope.size.width
    val h = scope.size.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.88f)
        cubicTo(w * 0.10f, h * 0.58f, w * 0.04f, h * 0.30f, w * 0.26f, h * 0.18f)
        cubicTo(w * 0.40f, h * 0.10f, w * 0.50f, h * 0.24f, w * 0.50f, h * 0.30f)
        cubicTo(w * 0.50f, h * 0.24f, w * 0.60f, h * 0.10f, w * 0.74f, h * 0.18f)
        cubicTo(w * 0.96f, h * 0.30f, w * 0.90f, h * 0.58f, w * 0.50f, h * 0.88f)
        close()
    }
}
