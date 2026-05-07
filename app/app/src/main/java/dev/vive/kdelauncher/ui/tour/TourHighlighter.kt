package dev.vive.kdelauncher.ui.tour

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun TourHighlighter(
    targetBounds: Rect?,
    modifier: Modifier = Modifier
) {
    if (targetBounds == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(TourDefaults.PulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val padding = 12.dp
    val cornerRadius = 16.dp

    with(LocalDensity.current) {
        val targetWidth = targetBounds.width.toDp() + (padding * 2)
        val targetHeight = targetBounds.height.toDp() + (padding * 2)
        val targetOffsetX = targetBounds.left.toDp() - padding
        val targetOffsetY = targetBounds.top.toDp() - padding
        val primaryColor = dev.vive.kdelauncher.ui.theme.LocalLauncherAccent.current.primary

        val width by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetWidth,
            animationSpec = tween(TourDefaults.AnimationDuration),
            label = "width"
        )
        val height by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(TourDefaults.AnimationDuration),
            label = "height"
        )
        val offsetX by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetOffsetX,
            animationSpec = tween(TourDefaults.AnimationDuration),
            label = "offsetX"
        )
        val offsetY by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetOffsetY,
            animationSpec = tween(TourDefaults.AnimationDuration),
            label = "offsetY"
        )

        Box(
            modifier = modifier
                .offset(x = offsetX, y = offsetY)
                .size(width = width, height = height)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    spotColor = primaryColor,
                    ambientColor = primaryColor
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(primaryColor.copy(alpha = pulseAlpha))
                .border(
                    width = 1.dp,
                    color = primaryColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}
