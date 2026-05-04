package dev.vive.kdelauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.data.model.AppModel
import dev.vive.kdelauncher.ui.theme.LauncherTypography
import dev.vive.kdelauncher.ui.theme.LocalColors

@Composable
fun AppGrid(
    apps: List<AppModel>,
    searchQuery: String,
    activeCategory: AppCategory,
    categoryConfigs: List<CategoryConfig>,
    visibleCategories: List<AppCategory>,
    showAppLabels: Boolean,
    iconSize: IconSize = IconSize.MEDIUM,
    showIconBackground: Boolean = true,
    gridColumns: Int = 3,
    onAppClick: (AppModel) -> Unit,
    onToggleFavorite: (AppModel) -> Unit,
    onAssignCategory: (AppModel, AppCategory) -> Unit,
    onClearCategory: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val config = categoryConfigs.find { it.category == activeCategory }
    val categoryName = config?.displayName ?: activeCategory.displayName

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Category header
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = categoryName,
                    style = LauncherTypography.titleMedium,
                    color = colors.onBackground
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Resultados",
                    style = LauncherTypography.titleMedium,
                    color = colors.onBackground
                )
            }
        }

        if (apps.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val msg = if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\""
                else if (activeCategory == AppCategory.FAVORITES) "Mantén presionado un app para agregarla a favoritos"
                else "No hay apps en esta categoría"

                Text(
                    text = msg,
                    style = LauncherTypography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            ) {
                items(apps, key = { "${it.packageName}:${it.profileTag.name}" }) { app ->
                    AppIcon(
                        app = app,
                        onClick = { onAppClick(app) },
                        onToggleFavorite = { onToggleFavorite(app) },
                        onAssignCategory = { category -> onAssignCategory(app, category) },
                        onClearCategory = { onClearCategory(app) },
                        activeCategory = activeCategory,
                        categoryConfigs = categoryConfigs,
                        visibleCategories = visibleCategories,
                        showLabel = showAppLabels,
                        iconSize = iconSize,
                        showIconBackground = showIconBackground
                    )
                }
            }
        }
    }
}
