package dev.vive.kdelauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vive.kdelauncher.ui.LauncherViewModel
import dev.vive.kdelauncher.ui.components.*
import dev.vive.kdelauncher.ui.theme.LauncherColors
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent
import androidx.compose.ui.zIndex
import dev.vive.kdelauncher.ui.tour.ProductTourOverlay
import dev.vive.kdelauncher.ui.tour.TourTarget
import dev.vive.kdelauncher.ui.tour.tourTarget

/**
 * Main launcher screen:
 * [Not-default banner] → ProfileHeader (with gear icon) → Settings panel (collapsible) →
 * SearchBar → [CategorySidebar | AppGrid]
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appGridState by viewModel.appGridState.collectAsStateWithLifecycle()
    val tourState by viewModel.tourState.collectAsStateWithLifecycle()
    val organizationSuggestionState by viewModel.organizationSuggestionState.collectAsStateWithLifecycle()
    val pendingInstallSuggestions by viewModel.pendingInstallSuggestions.collectAsStateWithLifecycle()
    val targetPositions = remember { androidx.compose.runtime.mutableStateMapOf<TourTarget, androidx.compose.ui.geometry.Rect>() }
    val resetCounter by viewModel.homeResetCounter.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val tempHiddenApps by viewModel.tempHiddenApps.collectAsStateWithLifecycle()
    val showAllHiddenTemporarily by viewModel.showAllHiddenTemporarily.collectAsStateWithLifecycle()
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Only attach tour modifiers when the tour is actually active.
    // This eliminates Modifier.Node overhead during normal usage.
    fun Modifier.tourIfActive(
        target: TourTarget,
        onPositioned: (TourTarget, androidx.compose.ui.geometry.Rect) -> Unit
    ): Modifier = if (tourState.isActive) {
        this.then(Modifier.tourTarget(target, tourState, onPositioned))
    } else {
        this
    }

    LaunchedEffect(resetCounter) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── "Not default launcher" warning banner ──────────────────────────
        AnimatedVisibility(
            visible = !uiState.isDefaultLauncher,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Row(
                modifier = Modifier
                    .tourIfActive(TourTarget.Banner) { t, r -> targetPositions[t] = r }
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                LauncherColors.AccentOrange.copy(alpha = 0.15f),
                                LauncherColors.AccentOrange.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .clickable { viewModel.openSetDefaultLauncherScreen() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = LauncherColors.AccentOrange
                )
                Text(
                    text = "No es el launcher predeterminado. Toca aquí para activarlo.",
                    fontSize = 12.sp,
                    color = LauncherColors.AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LauncherColors.AccentOrange.copy(alpha = 0.7f)
                )
            }
        }

        // ── New app categorization suggestions banner ──────────────────────
        AnimatedVisibility(
            visible = pendingInstallSuggestions.isNotEmpty(),
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            val firstSuggestion = pendingInstallSuggestions.firstOrNull()
            val suggestionText = if (pendingInstallSuggestions.size == 1 && firstSuggestion != null) {
                "${firstSuggestion.label} → ${dev.vive.kdelauncher.data.model.AppCategory.displayName(firstSuggestion.proposedCategory)}"
            } else {
                "${pendingInstallSuggestions.size} apps sin categoría"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                accent.primary.copy(alpha = 0.15f),
                                accent.primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accent.primary
                )
                Text(
                    text = "Sugerencia: $suggestionText",
                    fontSize = 12.sp,
                    color = accent.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { viewModel.applyOrganizationSuggestions(pendingInstallSuggestions) }
                ) {
                    Text("Aplicar", color = accent.primary, fontSize = 12.sp)
                }
                TextButton(
                    onClick = { viewModel.clearPendingInstallSuggestions() }
                ) {
                    Text("Ignorar", color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }

        // Profile header with gear icon for settings
        ProfileHeader(
            profile = uiState.currentProfile,
            onToggleProfile = { viewModel.toggleProfile() },
            onToggleSettings = { viewModel.toggleSettings() },
            showSettingsActive = uiState.showSettings,
            hasRealWorkProfile = uiState.hasRealWorkProfile,
            isWorkProfileLocked = uiState.isWorkProfileLocked,
            modifier = Modifier
                .tourIfActive(TourTarget.ProfileHeader) { t, r -> targetPositions[t] = r }
                .padding(top = 4.dp, bottom = 8.dp),
            settingsModifier = Modifier
                .tourIfActive(TourTarget.SettingsButton) { t, r -> targetPositions[t] = r }
        )

        // Settings panel (collapsible, max 60% screen height)
        AnimatedVisibility(
            visible = uiState.showSettings,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
        ) {
            LauncherSettingsPanel(
                isDarkTheme = uiState.isDarkTheme,
                colorTheme = uiState.colorTheme,
                showAppLabels = appGridState.showAppLabels,
                iconSize = appGridState.iconSize,
                showIconBackground = appGridState.showIconBackground,
                gridColumns = appGridState.gridColumns,
                categoryConfigs = appGridState.categoryConfigs,
                appCounts = appGridState.appCounts,
                installedIconPacks = uiState.installedIconPacks,
                selectedIconPack = uiState.selectedIconPack,
                isLoadingIconPacks = uiState.isLoadingIconPacks,
                onToggleTheme = { viewModel.toggleTheme() },
                onColorThemeChange = { viewModel.setColorTheme(it) },
                onToggleAppLabels = {
                    viewModel.setShowAppLabels(!appGridState.showAppLabels)
                },
                onIconSizeChange = { viewModel.setIconSize(it) },
                onIconBackgroundToggle = { viewModel.setShowIconBackground(!appGridState.showIconBackground) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onCategoryRename = { cat, name -> viewModel.setCategoryDisplayName(cat, name) },
                onCategoryIconChange = { cat, icon -> viewModel.setCategoryIconName(cat, icon) },
                onCategoryToggleHidden = { viewModel.toggleCategoryHidden(it) },
                onDeleteCategory = { cat, count -> viewModel.deleteCategory(cat, count) },
                onCategoryOrderChange = { viewModel.setCategoryOrder(it) },
                onAddCategory = { viewModel.addCustomCategory(it) },
                onSelectIconPack = { viewModel.setIconPack(it) },
                onReset = { viewModel.resetSettings() },
                onResetTour = {
                    if (uiState.showSettings) {
                        viewModel.toggleSettings()
                    }
                    viewModel.startProductTour()
                },
                organizationSuggestionState = organizationSuggestionState,
                onSuggestOrganization = { viewModel.suggestOrganization() },
                onApplyOrganizationSuggestions = { viewModel.applyOrganizationSuggestions(it) },
                onCancelOrganization = { viewModel.cancelOrganization() },
                hiddenApps = hiddenApps,
                tempHiddenApps = tempHiddenApps,
                allApps = uiState.allApps,
                onHideApp = { app, mins ->
                    if (mins == 0) viewModel.hideApp(app.packageName)
                    else if (mins == -1) viewModel.tempHideApp(app.packageName, Int.MAX_VALUE)
                    else viewModel.tempHideApp(app.packageName, mins)
                },
                onUnhideApp = { viewModel.unhideApp(it) },
                showAllHiddenTemporarily = showAllHiddenTemporarily,
                onToggleShowHidden = { viewModel.toggleShowHiddenTemporarily() },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .heightIn(max = 420.dp)
            )
        }

        // Search bar
        SearchBar(
            query = appGridState.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .tourIfActive(TourTarget.SearchBar) { t, r -> targetPositions[t] = r }
                .padding(bottom = 12.dp)
        )

        // Main content: Sidebar + App Grid
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.tourIfActive(TourTarget.CategorySidebar) { t, r -> targetPositions[t] = r }) {
                CategorySidebar(
                    activeCategory = appGridState.activeCategory,
                    visibleCategories = appGridState.visibleCategories,
                    categoryConfigs = appGridState.categoryConfigs,
                    onCategorySelected = { viewModel.setActiveCategory(it) }
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.border.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            AppGrid(
                apps = appGridState.filteredApps,
                searchQuery = appGridState.searchQuery,
                activeCategory = appGridState.activeCategory,
                categoryConfigs = appGridState.categoryConfigs,
                visibleCategories = appGridState.visibleCategories,
                showAppLabels = appGridState.showAppLabels,
                iconSize = appGridState.iconSize,
                showIconBackground = appGridState.showIconBackground,
                gridColumns = appGridState.gridColumns,
                onAppClick = { viewModel.launchApp(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAssignCategory = { app, category -> viewModel.setCategoryOverride(app, category) },
                onClearCategory = { viewModel.clearCategoryOverride(it) },
                onAppInfo = { viewModel.openAppInfo(it) },
                onUninstall = { viewModel.uninstallApp(it) },
                modifier = Modifier
                    .weight(1f)
                    .tourIfActive(TourTarget.AppGrid) { t, r -> targetPositions[t] = r }
            )
        }
    }

    }

    if (tourState.isActive) {
        ProductTourOverlay(
            tourState = tourState,
            targetPositions = targetPositions,
            onNext = { viewModel.nextTourStep() },
            onPrevious = { viewModel.previousTourStep() },
            onSkip = { viewModel.skipProductTour() },
            modifier = Modifier.zIndex(999f)
        )
    }
}




