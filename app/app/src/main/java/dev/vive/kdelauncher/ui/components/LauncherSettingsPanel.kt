package dev.vive.kdelauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vive.kdelauncher.data.IconPackInfo

import dev.vive.kdelauncher.ui.components.IconSize
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent
import dev.vive.kdelauncher.ui.theme.LauncherTypography
import java.util.Collections

/**
 * Data representing a category's current display configuration.
 */
data class CategoryConfig(
    val category: String,
    val displayName: String,
    val iconName: String,
    val isHidden: Boolean,
)

/**
 * Launcher settings panel — theme toggle, category editor, reset.
 */
@Composable
fun LauncherSettingsPanel(
    isDarkTheme: Boolean,
    colorTheme: dev.vive.kdelauncher.data.model.ColorTheme,
    showAppLabels: Boolean,
    iconSize: IconSize,
    showIconBackground: Boolean,
    gridColumns: Int,
    categoryConfigs: List<CategoryConfig>,
    appCounts: Map<String, Int>,
    installedIconPacks: List<IconPackInfo>,
    selectedIconPack: String?,
    isLoadingIconPacks: Boolean,
    labsEnabled: Boolean,
    aiProvider: dev.vive.kdelauncher.data.model.AiProviderType,
    aiConnectionState: dev.vive.kdelauncher.ui.AiConnectionState,
    aiModel: String,
    organizationState: dev.vive.kdelauncher.ui.OrganizationState,
    onToggleTheme: () -> Unit,
    onColorThemeChange: (dev.vive.kdelauncher.data.model.ColorTheme) -> Unit,
    onToggleAppLabels: () -> Unit,
    onIconSizeChange: (IconSize) -> Unit,
    onIconBackgroundToggle: () -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onCategoryRename: (String, String) -> Unit,
    onCategoryIconChange: (String, String) -> Unit,
    onCategoryToggleHidden: (String) -> Unit,
    onDeleteCategory: (String, Int) -> Unit,
    onCategoryOrderChange: (List<String>) -> Unit,
    onSelectIconPack: (String?) -> Unit,
    onReset: () -> Unit,
    onToggleLabs: (Boolean) -> Unit,
    onConnectAi: (dev.vive.kdelauncher.data.model.AiProviderType, String) -> Unit,
    onDisconnectAi: () -> Unit,
    onSetAiModel: (String) -> Unit,
    onOrganizeApps: () -> Unit,
    onApplySuggestions: (List<dev.vive.kdelauncher.data.model.AppCategorization>) -> Unit,
    onCancelOrganization: () -> Unit,
    onResetTour: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    val scrollState = rememberScrollState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header ──────────────────────────────────────
        Text(
            text = "Ajustes del launcher",
            style = LauncherTypography.titleMedium,
            color = colors.onBackground
        )

        // ── Theme Toggle ─────────────────────────────────
        SectionLabel("Apariencia")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme-toggle-row")
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleTheme
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primaryBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accent.primary
                    )
                }
                Column {
                    Text(
                        text = if (isDarkTheme) "Tema oscuro" else "Tema claro",
                        style = LauncherTypography.bodyMedium,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Toca para cambiar",
                        style = LauncherTypography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent.primary,
                    checkedTrackColor = accent.primaryBg,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                )
            )
        }

        // ── Tema de color Dev ────────────────────────────
        SectionLabel("Tema de color")

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dev.vive.kdelauncher.data.model.ColorTheme.entries.forEach { theme ->
                val isSelected = colorTheme == theme
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) accent.primary.copy(alpha = 0.2f) else colors.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent.primary else colors.border.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onColorThemeChange(theme) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(theme.backgroundArgb(isDarkTheme)))
                                .border(1.dp, colors.border.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(
                                        if (theme == dev.vive.kdelauncher.data.model.ColorTheme.SYSTEM) accent.primary
                                        else androidx.compose.ui.graphics.Color(theme.accentArgb(isDarkTheme))
                                    )
                                    .border(1.dp, colors.border.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                        Text(
                            text = theme.displayName,
                            style = LauncherTypography.bodySmall.copy(fontSize = 10.sp),
                            color = if (isSelected) accent.primary else colors.onBackground
                        )
                    }
                }
            }
        }

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── App Label Toggle ─────────────────────────────
        SectionLabel("Contenido")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleAppLabels
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primaryBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accent.primary
                    )
                }
                Column {
                    Text(
                        text = if (showAppLabels) "Mostrar nombres de apps"
                        else "Ocultar nombres de apps",
                        style = LauncherTypography.bodyMedium,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Toca para cambiar",
                        style = LauncherTypography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = showAppLabels,
                onCheckedChange = { onToggleAppLabels() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent.primary,
                    checkedTrackColor = accent.primaryBg,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                )
            )
        }

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── Icon Settings ─────────────────────────────────
        SectionLabel("Íconos")

        // Icon size selector
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Tamaño",
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconSize.values().forEach { size ->
                    val isSelected = iconSize == size
                    val sizeLabel = when (size) {
                        IconSize.SMALL -> "Pequeño"
                        IconSize.MEDIUM -> "Mediano"
                        IconSize.LARGE -> "Grande"
                    }
                    val (containerSize, _) = getIconDimensions(size)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("icon-size-${size.name.lowercase()}")
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) accent.primaryBg
                                else colors.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent.primary
                                else colors.border.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onIconSizeChange(size) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(containerSize.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (showIconBackground)
                                            colors.surfaceVariant.copy(alpha = 0.8f)
                                        else
                                            colors.surface.copy(alpha = 0.5f)
                                    )
                                    .then(
                                        if (showIconBackground) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = colors.border.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Android,
                                    contentDescription = null,
                                    modifier = Modifier.size((containerSize * 0.6f).dp),
                                    tint = colors.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = sizeLabel,
                                style = LauncherTypography.bodySmall,
                                color = if (isSelected) accent.primary else colors.onBackground
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Icon background toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onIconBackgroundToggle
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.primaryBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accent.primary
                    )
                }
                Column {
                    Text(
                        text = if (showIconBackground) "Mostrar fondo de íconos"
                        else "Ocultar fondo de íconos",
                        style = LauncherTypography.bodyMedium,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Toca para cambiar",
                        style = LauncherTypography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = showIconBackground,
                onCheckedChange = { onIconBackgroundToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent.primary,
                    checkedTrackColor = accent.primaryBg,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Grid columns selector
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Columnas de cuadrícula",
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val (minCols, maxCols) = calculateColumnRange(
                    iconSize = iconSize,
                    showIconBackground = showIconBackground,
                    screenWidthDp = screenWidthDp
                )
                (minCols..maxCols).forEach { cols ->
                    val isSelected = gridColumns == cols

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grid-columns-$cols")
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) accent.primaryBg
                                else colors.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent.primary
                                else colors.border.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onGridColumnsChange(cols) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$cols",
                                style = LauncherTypography.titleMedium,
                                color = if (isSelected) accent.primary else colors.onBackground
                            )
                            Text(
                                text = if (cols == 1) "columna" else "columnas",
                                style = LauncherTypography.bodySmall,
                                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            // Info text about column limits
            Text(
                text = "El rango de columnas se ajusta según el tamaño y fondo de íconos",
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── Categories ───────────────────────────────────
        SectionLabel("Categorías  •  flechas para reorderar")

        val orderedConfigs = remember { mutableStateListOf(*categoryConfigs.toTypedArray()) }

        LaunchedEffect(categoryConfigs) {
            orderedConfigs.clear()
            orderedConfigs.addAll(categoryConfigs)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            orderedConfigs.forEachIndexed { index, config ->
                val count = appCounts[config.category] ?: 0
                val isAll = config.category == dev.vive.kdelauncher.data.model.AppCategory.ALL
                val isFav = config.category == dev.vive.kdelauncher.data.model.AppCategory.FAVORITES
                CategorySettingsRow(
                    config = config,
                    appCount = count,
                    onRename = { onCategoryRename(config.category, it) },
                    onIconChange = { onCategoryIconChange(config.category, it) },
                    onToggleHidden = { onCategoryToggleHidden(config.category) },
                    onDelete = { onDeleteCategory(config.category, count) },
                    onMoveUp = {
                        if (index > 0) {
                            Collections.swap(orderedConfigs, index, index - 1)
                            onCategoryOrderChange(orderedConfigs.map { it.category })
                        }
                    },
                    onMoveDown = {
                        if (index < orderedConfigs.lastIndex) {
                            Collections.swap(orderedConfigs, index, index + 1)
                            onCategoryOrderChange(orderedConfigs.map { it.category })
                        }
                    },
                    isHideProtected = isAll,
                    isDeleteProtected = isAll || isFav,
                    isFirst = index == 0,
                    isLast = index == orderedConfigs.lastIndex,
                )
            }
        }

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── Icon Pack Selector ───────────────────────────
        SectionLabel("Pack de íconos")

        if (isLoadingIconPacks) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accent.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Buscando packs...",
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        } else if (installedIconPacks.isEmpty()) {
            // No packs found
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.Info, null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Column {
                    Text(
                        "No se encontraron packs",
                        style = LauncherTypography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        "Instala uno desde Play Store: Whicons, Delta Icons, Candycons...",
                        style = LauncherTypography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // "System default" option
                IconPackRow(
                    packName = "Íconos del sistema",
                    packIcon = null,
                    isSelected = selectedIconPack == null,
                    onClick = { onSelectIconPack(null) }
                )
                // Installed packs
                installedIconPacks.forEach { pack ->
                    IconPackRow(
                        packName = pack.label,
                        packIcon = pack.previewIcon,
                        isSelected = selectedIconPack == pack.packageName,
                        onClick = { onSelectIconPack(pack.packageName) }
                    )
                }
            }
        }

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── TAPO Labs ─────────────────────────────────────────────
        LabsSection(
            labsEnabled = labsEnabled,
            onToggleLabs = onToggleLabs,
            aiProvider = aiProvider,
            aiConnectionState = aiConnectionState,
            aiModel = aiModel,
            organizationState = organizationState,
            onConnectAi = onConnectAi,
            onDisconnectAi = onDisconnectAi,
            onSetAiModel = onSetAiModel,
            onOrganizeApps = onOrganizeApps,
            onApplySuggestions = onApplySuggestions,
            onCancelOrganization = onCancelOrganization
        )

        // ── Tour ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onResetTour
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant
                )
            }
            Text(
                text = "Volver a ver el tutorial",
                style = LauncherTypography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── Reset ─────────────────────────────────────────
        var showResetConfirm by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset-settings-row")
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showResetConfirm = true }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant
                )
            }
            Text(
                text = "Restaurar ajustes predeterminados",
                style = LauncherTypography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("¿Restaurar todo?") },
                text = { Text("Se perderán todos los cambios de nombres, íconos y tema.") },
                confirmButton = {
                    TextButton(onClick = {
                        onReset()
                        showResetConfirm = false
                    }) { Text("Restaurar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// ── Section label ────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    val colors = LocalColors.current
    Text(
        text = text.uppercase(),
        style = LauncherTypography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp
        ),
        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
    )
}

// ── One category row ─────────────────────────────────────────
@Composable
private fun CategorySettingsRow(
    config: CategoryConfig,
    appCount: Int,
    onRename: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isHideProtected: Boolean = false,
    isDeleteProtected: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    var showIconPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val contentAlpha = if (config.isHidden) 0.38f else 1f

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = if (showIconPicker) 0.6f else 0.3f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Icon button — clearly tappable ────────────────
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (showIconPicker) accent.primary.copy(alpha = 0.25f)
                        else accent.primaryBg
                    )
                    .border(
                        width = if (showIconPicker) 1.5.dp else 1.dp,
                        color = if (showIconPicker) accent.primary
                        else colors.border.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showIconPicker = !showIconPicker },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconResolver.resolve(config.iconName),
                    contentDescription = "Cambiar ícono",
                    modifier = Modifier.size(18.dp),
                    tint = accent.primary.copy(alpha = contentAlpha)
                )
            }

            // ── Name — tappable to rename ─────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showRenameDialog = true }
            ) {
                Text(
                    text = config.displayName,
                    style = LauncherTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.onBackground.copy(alpha = contentAlpha)
                )
                Text(
                    text = if (config.isHidden) "Oculta" else "Toca para renombrar",
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // ── Up/Down reorder ───────────────────────────
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Mover arriba",
                        modifier = Modifier.size(16.dp),
                        tint = if (isFirst) colors.onSurfaceVariant.copy(0.2f)
                        else colors.onSurfaceVariant.copy(0.5f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Mover abajo",
                        modifier = Modifier.size(16.dp),
                        tint = if (isLast) colors.onSurfaceVariant.copy(0.2f)
                        else colors.onSurfaceVariant.copy(0.5f)
                    )
                }
            }

            // ── Visibility toggle ─────────────────────────────
            IconButton(
                onClick = onToggleHidden,
                enabled = !isHideProtected,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (config.isHidden) Icons.Rounded.VisibilityOff
                    else Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (config.isHidden) colors.onSurfaceVariant.copy(0.4f)
                    else accent.primary.copy(alpha = 0.7f)
                )
            }

            // ── Delete button ──────────────────────────────
            if (!isDeleteProtected) {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar categoría",
                        modifier = Modifier.size(18.dp),
                        tint = colors.onSurfaceVariant.copy(0.5f)
                    )
                }
            }
        }

        // ── Icon picker grid (inline, animated) ───────────────
        AnimatedVisibility(
            visible = showIconPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Elige un ícono:",
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                // Fixed height grid (no lazy inside lazy)
                val allIcons = IconResolver.allEntries()
                val rows = allIcons.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (name, icon) ->
                                val isSelected = name == config.iconName
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) accent.primary.copy(alpha = 0.2f)
                                            else colors.surface.copy(alpha = 0.8f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) accent.primary
                                            else colors.border.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onIconChange(name)
                                            showIconPicker = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = name,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected) accent.primary
                                        else colors.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Rename dialog ─────────────────────────────────────────
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(config.displayName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar «${config.displayName}»") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Nombre de categoría") },
                    leadingIcon = {
                        Icon(
                            IconResolver.resolve(config.iconName),
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) onRename(newName.trim())
                    showRenameDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ── Delete confirmation dialog ─────────────────────────
    if (showDeleteDialog) {
        val isFavorites = config.category == dev.vive.kdelauncher.data.model.AppCategory.FAVORITES
        val message = if (appCount == 0) {
            "Eliminar la categoría «${config.displayName}»?"
        } else if (isFavorites) {
            "«${config.displayName}» tiene $appCount app${if (appCount == 1) "" else "s"}. Eliminar la categoría? Las apps se moverán a «Todas»."
        } else {
            "«${config.displayName}» tiene $appCount app${if (appCount == 1) "" else "s"}. Las apps se moverán a «Todas». Deseas eliminar esta categoría?"
        }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar categoría?") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Icon Pack Row ─────────────────────────────────────────────
@Composable
private fun IconPackRow(
    packName: String,
    packIcon: android.graphics.Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accent.primaryBg
                else colors.surfaceVariant.copy(alpha = 0.3f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accent.primary else colors.border.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pack icon preview
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (packIcon != null) {
                Image(
                    bitmap = packIcon.asImageBitmap(),
                    contentDescription = packName,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Pack name
        Text(
            text = packName,
            style = LauncherTypography.bodyMedium,
            color = if (isSelected) accent.primary else colors.onBackground,
            modifier = Modifier.weight(1f)
        )

        // Selected checkmark
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Seleccionado",
                modifier = Modifier.size(18.dp),
                tint = accent.primary
            )
        }
    }
}

/**
 * Calculates the valid range of grid columns based on icon size and background.
 *
 * This ensures that icons don't overflow or overlap by considering:
 * - Icon container size (based on icon size setting)
 * - Horizontal padding (constant 4dp per side = 8dp total per icon)
 * - Grid spacing (constant 4dp between icons)
 * - Minimum safe space per column to prevent overflow
 *
 * Returns Pair(minColumns, maxColumns)
 */
fun calculateColumnRange(
    iconSize: IconSize,
    showIconBackground: Boolean,
    screenWidthDp: Float
): Pair<Int, Int> {
    val (containerSize, _) = getIconDimensions(iconSize)

    // Calculate minimum space needed per column
    // Container size + horizontal padding (4dp each side) + some margin
    val minSpacePerColumn = containerSize + 8f + 4f // 8dp padding + 4dp margin

    // Calculate maximum possible columns
    val maxPossible = (screenWidthDp / minSpacePerColumn).toInt().coerceAtLeast(1)

    // Calculate minimum columns based on icon size
    // Larger icons need fewer columns minimum to prevent them from being too small
    val minColumns = when (iconSize) {
        IconSize.LARGE -> 2
        IconSize.MEDIUM -> 3
        IconSize.SMALL -> 4
    }

    // Adjust based on background visibility
    // Icons without background can be packed slightly tighter
    val adjustedMax = if (!showIconBackground) {
        maxPossible + 1
    } else {
        maxPossible
    }

    // Ensure we have at least a reasonable range
    val effectiveMin = minOf(minColumns, adjustedMax)
    val effectiveMax = maxOf(effectiveMin + 1, adjustedMax).coerceAtMost(6)

    return Pair(effectiveMin, effectiveMax)
}
