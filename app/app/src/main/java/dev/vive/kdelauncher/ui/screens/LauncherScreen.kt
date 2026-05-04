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
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vive.kdelauncher.ui.LauncherViewModel
import dev.vive.kdelauncher.ui.components.*
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent

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
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current

    Column(
        modifier = modifier
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
    }
}




