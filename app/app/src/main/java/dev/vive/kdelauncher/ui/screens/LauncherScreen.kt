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
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dev.vive.kdelauncher.ui.theme.LauncherTypography
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
    val uiState by viewModel.uiState.collectAsState()
    val tourState = uiState.tourState
    val targetPositions = remember { androidx.compose.runtime.mutableStateMapOf<TourTarget, androidx.compose.ui.geometry.Rect>() }
    val resetCounter by viewModel.homeResetCounter.collectAsState()
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(resetCounter) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            delay(1500) // Keep splash visible for a bit to allow GPU to settle
            showSplash = false
        }
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
                    .tourTarget(TourTarget.Banner, tourState) { t, r -> targetPositions[t] = r }
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF9100).copy(alpha = 0.15f),
                                Color(0xFFFF9100).copy(alpha = 0.05f)
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
                    tint = Color(0xFFFF9100)
                )
                Text(
                    text = "No es el launcher predeterminado. Toca aquí para activarlo.",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9100),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFF9100).copy(alpha = 0.7f)
                )
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
                .tourTarget(TourTarget.ProfileHeader, tourState) { t, r -> targetPositions[t] = r }
                .padding(top = 4.dp, bottom = 8.dp),
            settingsModifier = Modifier
                .tourTarget(TourTarget.SettingsButton, tourState) { t, r -> targetPositions[t] = r }
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
                showAppLabels = uiState.showAppLabels,
                iconSize = uiState.iconSize,
                showIconBackground = uiState.showIconBackground,
                gridColumns = uiState.gridColumns,
                categoryConfigs = uiState.categoryConfigs,
                installedIconPacks = uiState.installedIconPacks,
                selectedIconPack = uiState.selectedIconPack,
                isLoadingIconPacks = uiState.isLoadingIconPacks,
                labsEnabled = uiState.labsEnabled,
                aiProvider = uiState.aiProvider,
                aiConnectionState = uiState.aiConnectionState,
                aiModel = uiState.aiModel,
                organizationState = uiState.organizationState,
                onToggleTheme = { viewModel.toggleTheme() },
                onColorThemeChange = { viewModel.setColorTheme(it) },
                onToggleAppLabels = {
                    viewModel.setShowAppLabels(!uiState.showAppLabels)
                },
                onIconSizeChange = { viewModel.setIconSize(it) },
                onIconBackgroundToggle = { viewModel.setShowIconBackground(!uiState.showIconBackground) },
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onCategoryRename = { cat, name -> viewModel.setCategoryDisplayName(cat, name) },
                onCategoryIconChange = { cat, icon -> viewModel.setCategoryIconName(cat, icon) },
                onCategoryToggleHidden = { viewModel.toggleCategoryHidden(it) },
                onSelectIconPack = { viewModel.setIconPack(it) },
                onReset = { viewModel.resetSettings() },
                onToggleLabs = { viewModel.setLabsEnabled(it) },
                onConnectAi = { provider, key -> viewModel.connectAiProvider(provider, key) },
                onDisconnectAi = { viewModel.disconnectAiProvider() },
                onSetAiModel = { viewModel.setAiModel(it) },
                onOrganizeApps = { viewModel.organizeAppsWithAi() },
                onApplySuggestions = { viewModel.applyAiSuggestions(it) },
                onCancelOrganization = { viewModel.cancelAiOrganization() },
                onResetTour = {
                    if (uiState.showSettings) {
                        viewModel.toggleSettings()
                    }
                    viewModel.startProductTour()
                },
                modifier = Modifier
                    .tourTarget(TourTarget.Labs, tourState) { t, r -> targetPositions[t] = r }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .heightIn(max = 420.dp)
            )
        }

        // Search bar
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .tourTarget(TourTarget.SearchBar, tourState) { t, r -> targetPositions[t] = r }
                .padding(bottom = 12.dp)
        )

        // Main content: Sidebar + App Grid
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.tourTarget(TourTarget.CategorySidebar, tourState) { t, r -> targetPositions[t] = r }) {
                CategorySidebar(
                    activeCategory = uiState.activeCategory,
                    visibleCategories = uiState.visibleCategories,
                    categoryConfigs = uiState.categoryConfigs,
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
                apps = uiState.filteredApps,
                searchQuery = uiState.searchQuery,
                activeCategory = uiState.activeCategory,
                categoryConfigs = uiState.categoryConfigs,
                visibleCategories = uiState.visibleCategories,
                showAppLabels = uiState.showAppLabels,
                iconSize = uiState.iconSize,
                showIconBackground = uiState.showIconBackground,
                gridColumns = uiState.gridColumns,
                onAppClick = { viewModel.launchApp(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAssignCategory = { app, category -> viewModel.setCategoryOverride(app, category) },
                onClearCategory = { viewModel.clearCategoryOverride(it) },
                onAppInfo = { viewModel.openAppInfo(it) },
                onUninstall = { viewModel.uninstallApp(it) },
                modifier = Modifier
                    .weight(1f)
                    .tourTarget(TourTarget.AppGrid, tourState) { t, r -> targetPositions[t] = r }
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

    // ── Splash Screen Overlay ──────────────────────────────────────────────
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .pointerInput(Unit) {
                    // Intercept touch events while splash is visible
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(accent.primary, accent.primary.copy(alpha = 0.5f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Android, // Placeholder logo
                        contentDescription = "Launcher Logo",
                        modifier = Modifier.size(72.dp),
                        tint = colors.background
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "TAPO Launcher",
                    style = LauncherTypography.headlineMedium,
                    color = colors.onBackground,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preparando interfaz...",
                    style = LauncherTypography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}




