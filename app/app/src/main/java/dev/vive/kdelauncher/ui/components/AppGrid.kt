package dev.vive.kdelauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
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
    activeCategory: String,
    categoryConfigs: List<CategoryConfig>,
    visibleCategories: List<String>,
    showAppLabels: Boolean,
    iconSize: IconSize = IconSize.MEDIUM,
    showIconBackground: Boolean = true,
    gridColumns: Int = 3,
    onAppClick: (AppModel) -> Unit,
    onToggleFavorite: (AppModel) -> Unit,
    onAssignCategory: (AppModel, String) -> Unit,
    onClearCategory: (AppModel) -> Unit,
    onAppInfo: (AppModel) -> Unit,
    onUninstall: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val config = categoryConfigs.find { it.category == activeCategory }
    val categoryName = config?.displayName ?: AppCategory.displayName(activeCategory)

    val gridState = rememberLazyGridState()

    var menuApp by remember { mutableStateOf<AppModel?>(null) }
    var showCategoryPicker by remember { mutableStateOf<AppModel?>(null) }

    val categoryOptions = remember(visibleCategories, categoryConfigs) {
        visibleCategories
            .filter { it != AppCategory.FAVORITES && it != AppCategory.ALL }
            .map { cat ->
                val name = categoryConfigs.find { it.category == cat }?.displayName
                    ?: AppCategory.displayName(cat)
                cat to name
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
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
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            ) {
                items(apps, key = { "${it.packageName}:${it.profileTag.name}" }) { app ->
                    AppIcon(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongPress = { menuApp = app },
                        showLabel = showAppLabels,
                        iconSize = iconSize,
                        showIconBackground = showIconBackground
                    )
                }
            }
        }
    }

    val menuAppCurrent = menuApp
    if (menuAppCurrent != null) {
        val accent = LocalLauncherAccent.current
        val isFavorite = menuAppCurrent.isFavorite
        val showRemoveAction = activeCategory != AppCategory.ALL
        val onRemove = if (activeCategory == AppCategory.FAVORITES) {
            { onToggleFavorite(menuAppCurrent) }
        } else {
            { onClearCategory(menuAppCurrent) }
        }
        val favoriteIcon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder
        val menuShape = RoundedCornerShape(14.dp)
        val menuItemBg = colors.surfaceVariant.copy(alpha = 0.7f)

        DropdownMenu(
            expanded = true,
            onDismissRequest = { menuApp = null },
            offset = DpOffset(6.dp, 6.dp),
            modifier = Modifier
                .widthIn(min = 180.dp)
                .clip(menuShape)
                .background(colors.surface)
                .border(1.dp, colors.border.copy(alpha = 0.8f), menuShape)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(menuItemBg)
                            .clickable {
                                menuApp = null
                                onToggleFavorite(menuAppCurrent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = favoriteIcon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isFavorite) accent.primary else colors.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(menuItemBg)
                            .clickable {
                                menuApp = null
                                showCategoryPicker = menuAppCurrent
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.onSurfaceVariant
                        )
                    }

                    if (showRemoveAction) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(menuItemBg)
                                .clickable {
                                    menuApp = null
                                    onRemove()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = colors.border.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                DropdownMenuItem(
                    text = { Text("Información", style = LauncherTypography.bodyMedium) },
                    onClick = {
                        menuApp = null
                        onAppInfo(menuAppCurrent)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                )

                DropdownMenuItem(
                    text = { Text("Desinstalar", style = LauncherTypography.bodyMedium) },
                    onClick = {
                        menuApp = null
                        onUninstall(menuAppCurrent)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                )
            }
        }
    }

    val categoryApp = showCategoryPicker
    if (categoryApp != null) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = null },
            title = { Text("Elegir categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categoryOptions.forEach { (category, label) ->
                        TextButton(onClick = {
                            onAssignCategory(categoryApp, category)
                            showCategoryPicker = null
                        }) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryPicker = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
