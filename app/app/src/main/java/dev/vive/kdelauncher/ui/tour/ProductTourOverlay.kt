package dev.vive.kdelauncher.ui.tour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun ProductTourOverlay(
    tourState: TourState,
    targetPositions: Map<TourTarget, Rect>,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Scrim de fondo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TourDefaults.ScrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true,
                    onClick = {}
                )
        )
        
        // 2. Highlighter del paso actual
        val currentStep = tourState.currentStep()
        val rawTargetBounds = currentStep?.let { targetPositions[it.target] }
        
        var lastKnownBounds by remember { mutableStateOf<Rect?>(null) }
        if (rawTargetBounds != null) {
            lastKnownBounds = rawTargetBounds
        }
        val targetBounds = if (rawTargetBounds == null && tourState.isActive && currentStep?.target != TourTarget.None) lastKnownBounds else rawTargetBounds
        
        AnimatedVisibility(
            visible = targetBounds != null,
            enter = fadeIn(tween(TourDefaults.AnimationDuration)) + androidx.compose.animation.scaleIn(
                initialScale = 2.0f,
                animationSpec = tween(TourDefaults.AnimationDuration)
            ),
            exit = fadeOut(tween(TourDefaults.AnimationDuration)) + androidx.compose.animation.scaleOut(
                targetScale = 2.0f,
                animationSpec = tween(TourDefaults.AnimationDuration)
            )
        ) {
            TourHighlighter(targetBounds = targetBounds)
        }
        
        // 3. Tooltip posicionado
        currentStep?.let { step ->
            val density = LocalDensity.current
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val screenHeight = LocalConfiguration.current.screenHeightDp
            
            val tooltipPosition = remember(targetBounds, screenWidth, screenHeight) {
                calculateTooltipPosition(targetBounds, step, density, screenWidth, screenHeight)
            }
            
            val stepTitle = stringResource(id = step.titleRes)
            val stepDesc = stringResource(id = step.descriptionRes)

            val animatedTooltipPosition by androidx.compose.animation.core.animateIntOffsetAsState(
                targetValue = tooltipPosition,
                animationSpec = tween(TourDefaults.AnimationDuration),
                label = "tooltipOffset"
            )

            val tooltipModifier = Modifier.offset { animatedTooltipPosition }

            AnimatedVisibility(
                visible = tourState.isActive,
                enter = fadeIn(tween(TourDefaults.AnimationDuration)) + slideInVertically(
                    animationSpec = tween(TourDefaults.AnimationDuration),
                    initialOffsetY = { it / 4 }
                ),
                exit = fadeOut(tween(TourDefaults.AnimationDuration)),
                modifier = tooltipModifier
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "tooltipContent"
                ) { targetStep ->
                    val stepTitle = stringResource(id = targetStep.titleRes)
                    val stepDesc = stringResource(id = targetStep.descriptionRes)
                    
                    TourTooltip(
                        step = targetStep,
                        stepIndex = tourState.steps.indexOf(targetStep),
                        totalSteps = tourState.steps.size,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSkip = onSkip,
                        isFirst = tourState.steps.indexOf(targetStep) == 0,
                        isLast = tourState.steps.indexOf(targetStep) == tourState.steps.lastIndex,
                        modifier = Modifier.semantics {
                            contentDescription = "Tutorial de bienvenida. $stepTitle. $stepDesc"
                        }
                    )
                }
            }
        }
    }
}

private fun calculateTooltipPosition(
    targetBounds: Rect?,
    step: TourStep,
    density: Density,
    screenWidth: Int,
    screenHeight: Int
): IntOffset {
    with(density) {
        val screenWidthPx = screenWidth.dp.roundToPx()
        val screenHeightPx = screenHeight.dp.roundToPx()
        val marginPx = 16.dp.roundToPx()
        val tooltipWidthPx = TourDefaults.TooltipWidth.roundToPx()
        val gapPx = 16.dp.roundToPx()
        val tooltipHeightEstimate = 260.dp.roundToPx() // Promedio para centrar
        
        if (targetBounds == null || step.target == TourTarget.None) {
            val x = (screenWidthPx - tooltipWidthPx) / 2
            val y = (screenHeightPx - tooltipHeightEstimate) / 2
            return IntOffset(x, y)
        }
        
        return when (step.target) {
            is TourTarget.Banner, is TourTarget.ProfileHeader, is TourTarget.SearchBar -> {
                val x = ((screenWidthPx - tooltipWidthPx) / 2).coerceIn(marginPx, screenWidthPx - tooltipWidthPx - marginPx)
                val y = (targetBounds.bottom.toInt() + gapPx).coerceAtMost(screenHeightPx - tooltipHeightEstimate)
                IntOffset(x, y)
            }
            is TourTarget.SettingsButton -> {
                val x = (targetBounds.left.toInt() - tooltipWidthPx - marginPx).coerceAtLeast(marginPx)
                val y = targetBounds.top.toInt().coerceAtMost(screenHeightPx - tooltipHeightEstimate)
                IntOffset(x, y)
            }
            is TourTarget.CategorySidebar -> {
                val x = (targetBounds.right.toInt() + gapPx).coerceAtMost(screenWidthPx - tooltipWidthPx - marginPx)
                val y = targetBounds.top.toInt().coerceAtMost(screenHeightPx - tooltipHeightEstimate)
                IntOffset(x, y)
            }
            is TourTarget.AppGrid -> {
                val x = ((screenWidthPx - tooltipWidthPx) / 2).coerceIn(marginPx, screenWidthPx - tooltipWidthPx - marginPx)
                val y = (targetBounds.top.toInt() + gapPx).coerceAtMost(screenHeightPx - tooltipHeightEstimate)
                IntOffset(x, y)
            }
            is TourTarget.Labs -> {
                val y = (targetBounds.top.toInt() - gapPx - tooltipHeightEstimate).coerceAtLeast(marginPx)
                val x = ((screenWidthPx - tooltipWidthPx) / 2).coerceIn(marginPx, screenWidthPx - tooltipWidthPx - marginPx)
                IntOffset(x, y)
            }
            else -> IntOffset.Zero
        }
    }
}
