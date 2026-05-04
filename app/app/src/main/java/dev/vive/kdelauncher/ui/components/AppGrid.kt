package dev.vive.kdelauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent

@Composable
fun AppGrid(
    apps: List<AppModel>,
    searchQuery: String,
    activeCategory: AppCategory,
    appCounts: Map<AppCategory, Int>,
    categoryConfigs: List<CategoryConfig>,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onRequestIcon: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = categoryName,
                    style = LauncherTypography.titleMedium,
                    color = colors.onBackground
                )
                Text(
                    text = "${appCounts[activeCategory] ?: 0} apps",
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant
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
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${apps.size} encontradas",
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant
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
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            ) {
                items(apps, key = { "${it.packageName}:${it.profileTag.name}" }) { app ->
                    AppIcon(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                        onRequestIcon = onRequestIcon
                    )
                }
            }
        }
    }
}
