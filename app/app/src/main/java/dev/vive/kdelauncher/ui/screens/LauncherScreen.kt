package dev.vive.kdelauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vive.kdelauncher.ui.LauncherViewModel
import dev.vive.kdelauncher.ui.components.*
import dev.vive.kdelauncher.ui.theme.LocalColors

/**
 * Main launcher screen:
 * ProfileHeader (with gear icon) -> Settings panel (collapsible) ->
 * SearchBar -> [CategorySidebar | AppGrid]
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Profile header with gear icon for settings
        ProfileHeader(
            profile = uiState.currentProfile,
            onToggleProfile = { viewModel.toggleProfile() },
            onToggleSettings = { viewModel.toggleSettings() },
            showSettingsActive = uiState.showSettings,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Settings panel (collapsible, max 60% screen height)
        AnimatedVisibility(
            visible = uiState.showSettings,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
        ) {
            LauncherSettingsPanel(
                isDarkTheme = uiState.isDarkTheme,
                categoryConfigs = uiState.categoryConfigs,
                installedIconPacks = uiState.installedIconPacks,
                selectedIconPack = uiState.selectedIconPack,
                isLoadingIconPacks = uiState.isLoadingIconPacks,
                onToggleTheme = { viewModel.toggleTheme() },
                onCategoryRename = { cat, name -> viewModel.setCategoryDisplayName(cat, name) },
                onCategoryIconChange = { cat, icon -> viewModel.setCategoryIconName(cat, icon) },
                onCategoryToggleHidden = { viewModel.toggleCategoryHidden(it) },
                onSelectIconPack = { viewModel.setIconPack(it) },
                onReset = { viewModel.resetSettings() },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .heightIn(max = 420.dp)
            )
        }

        // Search bar
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Main content: Sidebar + App Grid
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CategorySidebar(
                activeCategory = uiState.activeCategory,
                visibleCategories = uiState.visibleCategories,
                categoryConfigs = uiState.categoryConfigs,
                onCategorySelected = { viewModel.setActiveCategory(it) }
            )

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
                appCounts = uiState.appCounts,
                categoryConfigs = uiState.categoryConfigs,
                onAppClick = { viewModel.launchApp(it) },
                onAppLongClick = { viewModel.toggleFavorite(it) },
                modifier = Modifier.weight(1f)
            )
        }

        // Home indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.onBackground.copy(alpha = 0.15f))
            )
        }
    }
}
