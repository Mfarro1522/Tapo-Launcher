package dev.vive.kdelauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.vive.kdelauncher.data.model.Profile
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.ui.theme.LauncherColors
import dev.vive.kdelauncher.ui.theme.LauncherTypography
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent

@Composable
fun ProfileHeader(
    profile: Profile,
    onToggleProfile: () -> Unit,
    onToggleSettings: () -> Unit,
    showSettingsActive: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = LocalLauncherAccent.current
    val colors = LocalColors.current

    val avatarBgStart by animateColorAsState(
        accent.primary.copy(alpha = 0.3f), tween(300), label = "s"
    )
    val avatarBgEnd by animateColorAsState(
        accent.primary.copy(alpha = 0.1f), tween(300), label = "e"
    )
    val statusDotColor by animateColorAsState(
        if (profile.type == ProfileType.PERSONAL) LauncherColors.StatusOnline
        else LauncherColors.StatusWork, tween(300), label = "d"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar + Name (tappable to switch profile)
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleProfile
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(avatarBgStart, avatarBgEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (profile.type == ProfileType.PERSONAL)
                            Icons.Rounded.Person else Icons.Rounded.Work,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accent.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = profile.name,
                    style = LauncherTypography.titleMedium,
                    color = colors.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Cambiar perfil",
                        style = LauncherTypography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Icon(
                        Icons.Rounded.ChevronRight, null,
                        modifier = Modifier.size(12.dp),
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }

        // Gear icon for settings
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (showSettingsActive) accent.primaryBg
                    else colors.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleSettings
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                modifier = Modifier.size(18.dp),
                tint = if (showSettingsActive) accent.primary else colors.onSurfaceVariant
            )
        }
    }
}
