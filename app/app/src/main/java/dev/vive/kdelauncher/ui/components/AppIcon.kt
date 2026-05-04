package dev.vive.kdelauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.data.model.ProfileType
import dev.vive.kdelauncher.ui.theme.LauncherColors
import dev.vive.kdelauncher.ui.theme.LauncherTypography
import dev.vive.kdelauncher.ui.theme.LocalColors

@Composable
fun AppIcon(
    app: AppModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRequestIcon: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100), label = "appScale"
    )

    val imageBitmap = remember(app.packageName, app.profileTag) {
        app.iconBitmap?.asImageBitmap()
    }

    LaunchedEffect(app.packageName, app.activityName, app.userHandle, app.iconBitmap) {
        if (app.iconBitmap == null) onRequestIcon(app)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try { awaitRelease() } finally { isPressed = false }
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val isWork = app.profileTag == ProfileType.WORK
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isWork) LauncherColors.AccentOrangeBgLight
                        else colors.surfaceVariant.copy(alpha = 0.8f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isWork) LauncherColors.AccentOrange.copy(alpha = 0.2f)
                        else colors.border.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = app.label,
                        modifier = Modifier.size(20.dp),
                        tint = if (isWork) LauncherColors.AccentOrange
                        else colors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            if (app.profileTag == ProfileType.WORK) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(LauncherColors.AccentOrange)
                )
            }
        }

        Text(
            text = app.label,
            style = LauncherTypography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 72.dp)
        )
    }
}
