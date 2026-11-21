package com.colortapz.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colortapz.game.GameMode
import com.colortapz.game.GameState
import com.colortapz.game.R

@Composable
fun MenuScreen(
    gameState: GameState,
    isSoundEnabled: Boolean,
    isHapticsEnabled: Boolean,
    onSelectMode: (GameMode) -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenThemes: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.menu_title),
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.menu_subtitle),
            color = Color(0xFFCBD5E1),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        ModeSelector(
            selected = gameState.gameMode,
            onSelectMode = onSelectMode
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = gameState.gameMode.descriptionText(),
            color = Color(0xFFA0AEC0),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (gameState.highScore > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.menu_best,
                    gameState.gameMode.label().uppercase(),
                    gameState.highScore
                ),
                color = Color(0xFFFFD700),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuCardButton(
                label = stringResource(R.string.nav_ranks),
                modifier = Modifier.weight(1f),
                onClick = onOpenLeaderboard
            ) { RanksGlyph() }
            MenuCardButton(
                label = stringResource(R.string.nav_badges),
                modifier = Modifier.weight(1f),
                onClick = onOpenAchievements
            ) { BadgeGlyph() }
            MenuCardButton(
                label = stringResource(R.string.nav_themes),
                modifier = Modifier.weight(1f),
                onClick = onOpenThemes
            ) { PaletteGlyph() }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = stringResource(R.string.play_mode, gameState.gameMode.label()),
            onClick = onPlay
        )
    }
}

@Composable
private fun ModeSelector(selected: GameMode, onSelectMode: (GameMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22FFFFFF))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        GameMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFFE94560) else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelectMode(mode) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun MenuCardButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22FFFFFF))
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
