package com.colortapz.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colortapz.game.AchievementId
import com.colortapz.game.GameMode
import com.colortapz.game.GameScoreStorage
import com.colortapz.game.LeaderboardEntry
import com.colortapz.game.R
import com.colortapz.game.ThemeCatalog
import com.colortapz.game.ThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LeaderboardDialog(
    storage: GameScoreStorage,
    currentMode: GameMode,
    onClose: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    val entries by produceState(emptyList<LeaderboardEntry>(), selectedMode) {
        value = withContext(Dispatchers.IO) { storage.getLeaderboard(selectedMode) }
    }

    DialogContainer(title = stringResource(R.string.leaderboard_title), onClose = onClose) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22FFFFFF))
                .padding(2.dp)
                .selectableGroup()
        ) {
            GameMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFE94560) else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { selectedMode = mode }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_empty),
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                color = when (index) {
                                    0 -> Color(0xFFFFD700)
                                    1 -> Color(0xFFC0C0C0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> Color.White
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.leaderboard_score, entry.score),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.leaderboard_detail,
                                        entry.maxCombo,
                                        entry.perfectTaps
                                    ),
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsDialog(
    storage: GameScoreStorage,
    onClose: () -> Unit
) {
    val unlocked by produceState(emptySet<AchievementId>()) {
        value = withContext(Dispatchers.IO) { storage.getUnlockedAchievements() }
    }

    DialogContainer(title = stringResource(R.string.achievements_title), onClose = onClose) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AchievementId.entries.forEach { item ->
                val isUnlocked = unlocked.contains(item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isUnlocked) Color(0x3338EF7D) else Color(0x15FFFFFF))
                        .border(
                            1.dp,
                            if (isUnlocked) Color(0xFF38EF7D) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) Color(0xFF38EF7D) else Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.iconSymbol,
                            fontSize = 16.sp,
                            color = if (isUnlocked) Color(0xFF0B0E14) else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.titleText(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.descriptionText(),
                            color = if (isUnlocked) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    if (isUnlocked) {
                        CheckGlyph(tint = Color(0xFF38EF7D), size = 20.dp)
                    } else {
                        LockGlyph(tint = Color(0xFF94A3B8), size = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemesDialog(
    storage: GameScoreStorage,
    currentThemeId: ThemeId,
    onSelectTheme: (ThemeId) -> Unit,
    onClose: () -> Unit
) {
    val unlockedIds by produceState(emptySet<ThemeId>()) {
        value = withContext(Dispatchers.IO) {
            ThemeCatalog.themes.keys.filter { storage.isThemeUnlocked(it) }.toSet()
        }
    }

    DialogContainer(title = stringResource(R.string.themes_title), onClose = onClose) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeCatalog.themes.values.forEach { theme ->
                val isUnlocked = unlockedIds.contains(theme.id)
                val isSelected = currentThemeId == theme.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0x3300ADB5) else Color(0x18FFFFFF))
                        .border(
                            1.5.dp,
                            if (isSelected) Color(0xFF00ADB5) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = isUnlocked) { onSelectTheme(theme.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            theme.targetPalette.take(3).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = theme.id.displayName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUnlocked) {
                                    stringResource(R.string.theme_unlocked)
                                } else {
                                    theme.id.hintText()
                                },
                                color = if (isUnlocked) Color(0xFF38EF7D) else Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                    }

                    when {
                        isSelected -> Text(
                            text = stringResource(R.string.theme_equipped),
                            color = Color(0xFF00ADB5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        !isUnlocked -> LockGlyph(tint = Color(0xFF94A3B8), size = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogContainer(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050810))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1F2C))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                CircleIconButton(
                    contentDescription = stringResource(R.string.cd_close),
                    onClick = onClose
                ) {
                    CloseGlyph()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}
