package com.colortapz.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colortapz.game.AchievementId
import com.colortapz.game.GameMode
import com.colortapz.game.R
import com.colortapz.game.ThemeId

/**
 * 48dp round icon button. Material's IconButton already guarantees the 48dp
 * minimum touch target and a ripple; the old hand-rolled Boxes were 34dp with
 * indication = null, so they were too small to hit and gave no touch feedback.
 */
@Composable
fun CircleIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) Color(0x33FFFFFF) else Color(0x15FFFFFF))
            .semantics { this.contentDescription = contentDescription }
    ) {
        content()
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE94560),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Localised display name for a game mode. */
@Composable
fun GameMode.label(): String = when (this) {
    GameMode.CLASSIC -> stringResource(R.string.mode_classic)
    GameMode.TIME_ATTACK -> stringResource(R.string.mode_time_attack)
    GameMode.ZEN -> stringResource(R.string.mode_zen)
}

/** Localised one-line description for a game mode. */
@Composable
fun GameMode.descriptionText(): String = when (this) {
    GameMode.CLASSIC -> stringResource(R.string.mode_classic_desc)
    GameMode.TIME_ATTACK -> stringResource(R.string.mode_time_attack_desc)
    GameMode.ZEN -> stringResource(R.string.mode_zen_desc)
}

/** Localised achievement title. */
@Composable
fun AchievementId.titleText(): String = stringResource(
    when (this) {
        AchievementId.COMBO_MASTER -> R.string.ach_combo_master
        AchievementId.CENTURION -> R.string.ach_centurion
        AchievementId.FREEZE_SPECIALIST -> R.string.ach_freeze_specialist
        AchievementId.ZEN_MASTER -> R.string.ach_zen_master
        AchievementId.UNTOUCHABLE -> R.string.ach_untouchable
        AchievementId.SPEED_DEMON -> R.string.ach_speed_demon
        AchievementId.SHIELD_BREAKER -> R.string.ach_shield_breaker
    }
)

/** Localised achievement description. */
@Composable
fun AchievementId.descriptionText(): String = stringResource(
    when (this) {
        AchievementId.COMBO_MASTER -> R.string.ach_combo_master_desc
        AchievementId.CENTURION -> R.string.ach_centurion_desc
        AchievementId.FREEZE_SPECIALIST -> R.string.ach_freeze_specialist_desc
        AchievementId.ZEN_MASTER -> R.string.ach_zen_master_desc
        AchievementId.UNTOUCHABLE -> R.string.ach_untouchable_desc
        AchievementId.SPEED_DEMON -> R.string.ach_speed_demon_desc
        AchievementId.SHIELD_BREAKER -> R.string.ach_shield_breaker_desc
    }
)

/** Localised unlock hint for a locked theme. */
@Composable
fun ThemeId.hintText(): String = stringResource(
    when (this) {
        ThemeId.CLASSIC_NEON -> R.string.theme_hint_default
        ThemeId.CYBERPUNK -> R.string.theme_hint_combo
        ThemeId.PASTEL_DREAM -> R.string.theme_hint_games
        ThemeId.SUNSET_GLOW -> R.string.theme_hint_50
        ThemeId.RETRO_ARCADE -> R.string.theme_hint_100
    }
)
