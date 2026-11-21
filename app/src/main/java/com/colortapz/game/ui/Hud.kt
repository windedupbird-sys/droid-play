package com.colortapz.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colortapz.game.GameMode
import com.colortapz.game.R

@Composable
fun Hud(
    score: Int,
    lives: Int,
    maxLives: Int,
    timeRemainingMs: Long,
    gameMode: GameMode,
    combo: Int,
    multiplier: Int,
    freezeRemainingMs: Long,
    feverRemainingMs: Long,
    isSoundEnabled: Boolean,
    isHapticsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$score",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (multiplier > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hud_multiplier, multiplier),
                            color = Color(0xFFFFD700),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                if (combo > 1) {
                    Text(
                        text = stringResource(R.string.hud_combo, combo),
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    contentDescription = stringResource(
                        if (isSoundEnabled) R.string.cd_sound_on else R.string.cd_sound_off
                    ),
                    onClick = onToggleSound,
                    active = isSoundEnabled
                ) {
                    SoundGlyph(enabled = isSoundEnabled)
                }
                Spacer(modifier = Modifier.width(4.dp))
                CircleIconButton(
                    contentDescription = stringResource(
                        if (isHapticsEnabled) R.string.cd_haptics_on else R.string.cd_haptics_off
                    ),
                    onClick = onToggleHaptics,
                    active = isHapticsEnabled
                ) {
                    HapticsGlyph(enabled = isHapticsEnabled)
                }
                Spacer(modifier = Modifier.width(4.dp))
                CircleIconButton(
                    contentDescription = stringResource(R.string.cd_pause),
                    onClick = onPauseClick
                ) {
                    PauseGlyph()
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (gameMode) {
                GameMode.CLASSIC -> HeartsRow(lives = lives, maxLives = maxLives)
                GameMode.TIME_ATTACK -> {
                    val secs = (timeRemainingMs / 1000f + 0.9f).toInt()
                    StatusChip(
                        text = stringResource(R.string.hud_seconds, secs),
                        color = if (secs <= 10) Color(0xFFFF5252) else Color.White,
                        background = if (secs <= 10) Color(0x66FF1744) else Color(0x3300ADB5)
                    )
                }
                GameMode.ZEN -> StatusChip(
                    text = stringResource(R.string.hud_zen),
                    color = Color(0xFFBAE1FF),
                    background = Color(0x33BAE1FF)
                )
            }

            if (freezeRemainingMs > 0) {
                StatusChip(
                    text = stringResource(
                        R.string.hud_frozen,
                        (freezeRemainingMs / 1000f + 0.1f).toInt()
                    ),
                    color = Color(0xFF64DFDF),
                    background = Color(0x4464DFDF)
                )
            }
            if (feverRemainingMs > 0) {
                StatusChip(
                    text = stringResource(
                        R.string.hud_fever,
                        (feverRemainingMs / 1000f + 0.1f).toInt()
                    ),
                    color = Color(0xFFFFD700),
                    background = Color(0x55FFA000)
                )
            }
        }
    }
}

@Composable
private fun HeartsRow(lives: Int, maxLives: Int) {
    val filledLabel = stringResource(R.string.hud_heart_filled)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        // One announcement for the whole row instead of five separate glyphs.
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$lives / $maxLives $filledLabel"
        }
    ) {
        for (i in 0 until maxLives) {
            HeartGlyph(
                filled = i < lives,
                tint = if (i < lives) Color(0xFFFF4D6D) else Color(0x55FF4D6D),
                size = 18.dp
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color, background: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
