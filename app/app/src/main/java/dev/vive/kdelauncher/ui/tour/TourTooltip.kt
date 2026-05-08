package dev.vive.kdelauncher.ui.tour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vive.kdelauncher.ui.theme.LauncherTypography
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent

@Composable
fun TourTooltip(
    step: TourStep,
    stepIndex: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    
    // We use the theme's surface color but a bit more elevated
    val bgColor = colors.surfaceVariant
    val onBgColor = colors.onBackground
    val mutedColor = colors.onSurfaceVariant
    val primary = accent.primary

    Card(
        modifier = modifier.width(TourDefaults.TooltipWidth),
        shape = RoundedCornerShape(TourDefaults.TooltipCornerRadius),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Close/Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Saltar",
                        tint = mutedColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Título
            Text(
                text = stringResource(id = step.titleRes),
                style = LauncherTypography.titleLarge,
                color = onBgColor,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Descripción
            Text(
                text = stringResource(id = step.descriptionRes),
                style = LauncherTypography.bodyMedium,
                color = mutedColor,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Controles inferiores: Atrás - Dots - Siguiente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Atrás (Arrow)
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (!isFirst) {
                        IconButton(
                            onClick = onPrevious,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = mutedColor)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                }

                // Indicador de progreso (dots)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        val dotColor = if (index == stepIndex) primary else mutedColor.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == stepIndex) 8.dp else 6.dp)
                                .background(color = dotColor, shape = CircleShape)
                        )
                    }
                }

                // Siguiente / Listo (Arrow/Check)
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(44.dp)
                        .background(primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = if (isLast) "¡Entendido!" else "Siguiente",
                        tint = colors.background // Contrast against primary
                    )
                }
            }
        }
    }
}
