package com.colortapz.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colortapz.game.AchievementId
import com.colortapz.game.GameMode
import com.colortapz.game.GameStats
import com.colortapz.game.R

@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0B0E14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2430))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.game_paused),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = stringResource(R.string.action_resume), onClick = onResume)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(text = stringResource(R.string.action_restart), onClick = onRestart)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(text = stringResource(R.string.action_main_menu), onClick = onMenu)
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    highScore: Int,
    stats: GameStats,
    mode: GameMode,
    newlyUnlocked: List<AchievementId>,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(26.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.game_over),
            color = Color(0xFFFF4D6D),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = mode.label().uppercase(),
            color = Color(0xFF90A4AE),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "$score",
            color = Color.White,
            fontSize = 50.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = if (score >= highScore && score > 0) {
                stringResource(R.string.new_high_score)
            } else {
                stringResource(R.string.best_score, highScore)
            },
            color = Color(0xFFFFD700),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        if (newlyUnlocked.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFD700))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.achievement_unlocked),
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                newlyUnlocked.forEach { ach ->
                    Text(
                        text = "${ach.iconSymbol} ${ach.titleText()}: ${ach.descriptionText()}",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22FFFFFF))
                .padding(14.dp)
        ) {
            StatRow(stringResource(R.string.stat_total_taps), "${stats.totalTaps}")
            StatRow(stringResource(R.string.stat_perfect_taps), "${stats.perfectTaps}")
            StatRow(stringResource(R.string.stat_highest_combo), "${stats.maxCombo}x")
            StatRow(stringResource(R.string.stat_freeze_hits), "${stats.freezeTargetsTapped}")
            StatRow(stringResource(R.string.stat_armor_broken), "${stats.shieldedTargetsBroken}")
            StatRow(stringResource(R.string.stat_missed), "${stats.targetsMissed}")
        }

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = stringResource(R.string.action_play_again), onClick = onPlayAgain)
        Spacer(modifier = Modifier.height(10.dp))
        SecondaryButton(text = stringResource(R.string.action_main_menu), onClick = onMenu)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
