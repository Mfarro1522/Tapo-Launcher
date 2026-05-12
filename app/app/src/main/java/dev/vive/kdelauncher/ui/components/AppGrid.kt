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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
    val density = LocalDensity.current
    val config = categoryConfigs.find { it.category == activeCategory }
    val categoryName = remember(config, activeCategory) {
        config?.displayName ?: AppCategory.displayName(activeCategory)
    }

    val gridState = rememberLazyGridState()

    var menuApp by remember { mutableStateOf<AppModel?>(null) }
    var menuBounds by remember { mutableStateOf<Rect?>(null) }
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

    val stableOnAppClick: (AppModel) -> Unit = remember(onAppClick) { onAppClick }
    val stableOnLongPress: (AppModel) -> Unit = remember(onToggleFavorite) { onToggleFavorite }
    val stableOnAssignCategory: (AppModel, String) -> Unit = remember(onAssignCategory) { onAssignCategory }
    val stableOnClearCategory: (AppModel) -> Unit = remember(onClearCategory) { onClearCategory }
    val stableOnAppInfo: (AppModel) -> Unit = remember(onAppInfo) { onAppInfo }
    val stableOnUninstall: (AppModel) -> Unit = remember(onUninstall) { onUninstall }

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
                items(
                    apps,
                    key = { "${it.packageName}:${it.profileTag.name}" },
                    contentType = { "app_icon" }
                ) { app ->
                    // Capture layout coords for context menu positioning.
                    // onPlaced fires only when the item's placement changes
                    // (not on every layout pass like onGloballyPositioned),
                    // reducing per-frame overhead during scroll.
                    val coordsRef = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
                    Box(
                        modifier = Modifier.onPlaced { coords ->
                            coordsRef[0] = coords
                        }
                    ) {
                        val onClick = remember(app) { { stableOnAppClick(app) } }
                        val onLongPress = remember(app) {
                            {
                                menuApp = app
                                menuBounds = coordsRef[0]?.boundsInWindow()
                            }
                        }
                        AppIcon(
                            app = app,
                            onClick = onClick,
                            onLongPress = onLongPress,
                            showLabel = showAppLabels,
                            iconSize = iconSize,
                            showIconBackground = showIconBackground,
                        )
                    }
                }
            }
        }
    }

    val menuAppCurrent = menuApp
    val menuBoundsCurrent = menuBounds
    if (menuAppCurrent != null && menuBoundsCurrent != null) {
        AppContextMenu(
            app = menuAppCurrent,
            anchorBounds = menuBoundsCurrent,
            density = density,
            activeCategory = activeCategory,
            onDismiss = { menuApp = null; menuBounds = null },
            onToggleFavorite = { stableOnLongPress(menuAppCurrent) },
            onMove = { showCategoryPicker = menuAppCurrent; menuApp = null; menuBounds = null },
            onAppInfo = { stableOnAppInfo(menuAppCurrent); menuApp = null; menuBounds = null },
            onUninstall = { stableOnUninstall(menuAppCurrent); menuApp = null; menuBounds = null },
            onRemove = {
                if (activeCategory == AppCategory.FAVORITES) {
                    stableOnLongPress(menuAppCurrent)
                } else {
                    stableOnClearCategory(menuAppCurrent)
                }
                menuApp = null; menuBounds = null
            },
        )
    }

    val categoryApp = showCategoryPicker
    if (categoryApp != null) {
        val accent = LocalLauncherAccent.current
        AlertDialog(
            onDismissRequest = { showCategoryPicker = null },
            title = { Text("Mover a categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryOptions.forEach { (category, label) ->
                        val iconName = categoryConfigs.find { it.category == category }?.iconName
                            ?: AppCategory.defaultIcon(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.35f))
                                .clickable {
                                    stableOnAssignCategory(categoryApp, category)
                                    showCategoryPicker = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = IconResolver.resolve(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = accent.primary
                            )
                            Text(
                                text = label,
                                style = LauncherTypography.bodyMedium,
                                color = colors.onBackground
                            )
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

@Composable
private fun AppContextMenu(
    app: AppModel,
    anchorBounds: Rect,
    density: Density,
    activeCategory: String,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMove: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    val isFavorite = app.isFavorite
    val showRemoveAction = activeCategory != AppCategory.ALL
    val favoriteIcon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder
    val menuShape = RoundedCornerShape(14.dp)
    val menuItemBg = colors.surfaceVariant.copy(alpha = 0.7f)

    Popup(
        popupPositionProvider = AppMenuPositionProvider(anchorBounds, density),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 180.dp, max = 260.dp)
                .shadow(8.dp, menuShape)
                .clip(menuShape)
                .background(colors.surface)
                .border(1.dp, colors.border.copy(alpha = 0.8f), menuShape)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(menuItemBg)
                        .clickable {
                            onDismiss()
                            onToggleFavorite()
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = favoriteIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isFavorite) accent.primary else colors.onSurfaceVariant
                    )
                    Text(
                        text = if (isFavorite) "Quitar" else "Favorito",
                        style = LauncherTypography.bodySmall.copy(fontSize = 10.sp),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(menuItemBg)
                        .clickable {
                            onDismiss()
                            onMove()
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.onSurfaceVariant
                    )
                    Text(
                        text = "Mover",
                        style = LauncherTypography.bodySmall.copy(fontSize = 10.sp),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (showRemoveAction) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(menuItemBg)
                            .clickable {
                                onDismiss()
                                onRemove()
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.onSurfaceVariant
                        )
                        Text(
                            text = "Quitar",
                            style = LauncherTypography.bodySmall.copy(fontSize = 10.sp),
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = colors.border.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onDismiss()
                        onAppInfo()
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.onSurfaceVariant
                )
                Text("Información", style = LauncherTypography.bodyMedium, color = colors.onBackground)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onDismiss()
                        onUninstall()
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text("Desinstalar", style = LauncherTypography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private class AppMenuPositionProvider(
    private val anchorBounds: Rect,
    private val density: Density
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val anchor = with(density) {
            IntRect(
                left = this@AppMenuPositionProvider.anchorBounds.left.toDp().roundToPx(),
                top = this@AppMenuPositionProvider.anchorBounds.top.toDp().roundToPx(),
                right = this@AppMenuPositionProvider.anchorBounds.right.toDp().roundToPx(),
                bottom = this@AppMenuPositionProvider.anchorBounds.bottom.toDp().roundToPx()
            )
        }
        val margin = with(density) { 8.dp.roundToPx() }

        var x = anchor.left + (anchor.width - popupContentSize.width) / 2
        var y = anchor.bottom + margin

        if (x + popupContentSize.width > windowSize.width - margin) {
            x = windowSize.width - popupContentSize.width - margin
        }
        if (x < margin) x = margin

        if (y + popupContentSize.height > windowSize.height - margin) {
            y = anchor.top - popupContentSize.height - margin
        }
        if (y < margin) y = anchor.bottom + margin

        return IntOffset(x, y)
    }
}
