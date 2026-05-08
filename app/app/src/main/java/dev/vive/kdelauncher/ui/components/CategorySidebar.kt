package dev.vive.kdelauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent

@Composable
fun CategorySidebar(
    activeCategory: String,
    visibleCategories: List<String>,
    categoryConfigs: List<CategoryConfig>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalLauncherAccent.current
    val colors = LocalColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        visibleCategories.forEach { category ->
            val isActive = category == activeCategory
            val config = categoryConfigs.find { it.category == category }
            val iconName = config?.iconName ?: AppCategory.defaultIcon(category)

            val bgColor by animateColorAsState(
                if (isActive) accent.primary.copy(alpha = 0.15f) else Color.Transparent,
                tween(200), label = "bg"
            )
            val iconColor by animateColorAsState(
                if (isActive) accent.primary else colors.onSurfaceVariant,
                tween(200), label = "ic"
            )
            val indicatorColor by animateColorAsState(
                if (isActive) accent.primary else Color.Transparent,
                tween(200), label = "ind"
            )

            Box(
                Modifier
                    .padding(vertical = 2.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCategorySelected(category) }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier
                        .width(3.dp).height(20.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(indicatorColor)
                        .align(Alignment.CenterStart)
                )
                Box(
                    Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = config?.displayName ?: AppCategory.displayName(category),
                        modifier = Modifier.size(18.dp),
                        tint = iconColor
                    )
                }
            }
        }
    }
}
