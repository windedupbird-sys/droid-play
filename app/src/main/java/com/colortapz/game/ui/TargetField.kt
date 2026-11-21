package com.colortapz.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colortapz.game.R
import com.colortapz.game.ScorePopup
import com.colortapz.game.TapParticle
import com.colortapz.game.Target
import com.colortapz.game.TargetType

/**
 * Drives the render clock from the compositor instead of a 16ms polling loop.
 * One state write per frame, and only the composables that read it recompose.
 */
@Composable
fun rememberFrameTimeMillis(clock: () -> Long): Long {
    var time by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(clock) {
        while (true) {
            withFrameNanos { time = clock() }
        }
    }
    return time
}

@Composable
fun TargetField(
    targets: List<Target>,
    particles: List<TapParticle>,
    popups: List<ScorePopup>,
    isFrozen: Boolean,
    isFeverActive: Boolean,
    clock: () -> Long,
    onTargetTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = rememberFrameTimeMillis(clock)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = maxWidth
        val heightPx = maxHeight

        // Was a fixed 74dp: fine on a phone, tiny on a 12" tablet. Scale with the
        // short side of the play area instead.
        val shortSide = minOf(maxWidth.value, maxHeight.value)
        val targetSize = (shortSide * 0.19f).coerceIn(64f, 132f).dp

        targets.forEach { target ->
            key(target.id) {
                TargetBubble(
                    target = target,
                    now = now,
                    isFrozen = isFrozen,
                    isFeverActive = isFeverActive,
                    targetSize = targetSize,
                    widthPx = widthPx.value,
                    heightPx = heightPx.value,
                    onTap = { onTargetTap(target.id) }
                )
            }
        }

        particles.forEach { particle ->
            key(particle.id) {
                val age = (now - particle.spawnedAtMs).coerceAtLeast(0L)
                val progress = (age.toFloat() / particle.lifetimeMs).coerceIn(0f, 1f)
                val alpha = (1f - progress).coerceIn(0f, 1f)
                val travel = progress * 48f

                Box(
                    modifier = Modifier
                        .offset(
                            x = (widthPx.value * particle.xFraction + particle.vx * travel).dp,
                            y = (heightPx.value * particle.yFraction + particle.vy * travel).dp
                        )
                        .size((7f * (1f - progress * 0.5f)).dp)
                        .clip(CircleShape)
                        .background(particle.color.copy(alpha = alpha))
                )
            }
        }

        popups.forEach { popup ->
            key(popup.id) {
                val age = (now - popup.spawnedAtMs).coerceAtLeast(0L)
                val progress = (age.toFloat() / popup.lifetimeMs).coerceIn(0f, 1f)
                val alpha = (1f - progress).coerceIn(0f, 1f)
                val label = if (popup.textRes != 0) stringResource(popup.textRes) else popup.text

                Box(
                    modifier = Modifier.offset(
                        x = widthPx * popup.xFraction - 40.dp,
                        y = heightPx * popup.yFraction - 55.dp * progress
                    )
                ) {
                    Text(
                        text = label,
                        color = popup.color.copy(alpha = alpha),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        // Popup used to be drawn in the target's own colour with no
                        // outline, so "+9" on a gold target was near invisible.
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f * alpha),
                                offset = Offset(1.5f, 1.5f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetBubble(
    target: Target,
    now: Long,
    isFrozen: Boolean,
    isFeverActive: Boolean,
    targetSize: androidx.compose.ui.unit.Dp,
    widthPx: Float,
    heightPx: Float,
    onTap: () -> Unit
) {
    val rawElapsed = (now - target.spawnedAtMs).coerceAtLeast(0L)
    val effectiveElapsed = if (isFrozen) rawElapsed / 2 else rawElapsed
    val progress = (effectiveElapsed.toFloat() / target.maxLifetimeMs).coerceIn(0f, 1f)

    // Direct computation instead of animateFloatAsState: the animation clock was
    // fighting the 16ms poll and creating one animatable per target.
    val scale = 1f - progress * 0.30f
    val alpha = (1f - progress * 0.45f).coerceIn(0.2f, 1f)

    val borderWidth = when {
        isFeverActive -> 3.5.dp
        target.isShielded -> 3.5.dp
        target.type == TargetType.GOLDEN || target.type == TargetType.BOMB -> 3.dp
        else -> 1.8.dp
    }

    val borderColor = when {
        isFeverActive -> Color(0xFFFFD700)
        target.isShielded && target.isCracked -> Color(0xFFFF5252)
        target.isShielded -> Color(0xFFE0E0E0)
        target.type == TargetType.GOLDEN -> Color(0xFFFFEB3B)
        target.type == TargetType.BOMB -> Color(0xFFFF1744)
        target.type == TargetType.FREEZE -> Color(0xFFE0F7FA)
        target.type == TargetType.HEART -> Color(0xFFFF80AB)
        else -> Color.White.copy(alpha = 0.65f)
    }

    val label = stringResource(
        when (target.type) {
            TargetType.STANDARD -> R.string.target_standard
            TargetType.GOLDEN -> R.string.target_golden
            TargetType.HEART -> R.string.target_heart
            TargetType.FREEZE -> R.string.target_freeze
            TargetType.BOMB -> R.string.target_bomb
        }
    )

    val symbol = when {
        target.isShielded && target.isCracked -> "⚡"
        target.isShielded -> "⛨"
        isFeverActive -> "✦"
        else -> target.type.iconSymbol
    }

    Box(
        modifier = Modifier
            .offset(
                x = widthPx.dp * target.xFraction - targetSize / 2,
                y = heightPx.dp * target.yFraction - targetSize / 2
            )
            .size(targetSize * scale)
            .clip(CircleShape)
            .background(
                if (isFeverActive) Color(0xFFFFD700).copy(alpha = alpha)
                else target.color.copy(alpha = alpha)
            )
            .border(borderWidth, borderColor.copy(alpha = alpha), CircleShape)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = Color.White.copy(alpha = alpha),
            fontSize = if (target.type == TargetType.STANDARD && !target.isShielded) 20.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
