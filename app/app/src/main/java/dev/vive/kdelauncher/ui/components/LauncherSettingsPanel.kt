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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vive.kdelauncher.data.IconPackInfo
import dev.vive.kdelauncher.data.model.AppCategory
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent
import dev.vive.kdelauncher.ui.theme.LauncherTypography

/**
 * Data representing a category's current display configuration.
 */
data class CategoryConfig(
    val category: AppCategory,
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
    categoryConfigs: List<CategoryConfig>,
    installedIconPacks: List<IconPackInfo>,
    selectedIconPack: String?,
    isLoadingIconPacks: Boolean,
    onToggleTheme: () -> Unit,
    onCategoryRename: (AppCategory, String) -> Unit,
    onCategoryIconChange: (AppCategory, String) -> Unit,
    onCategoryToggleHidden: (AppCategory) -> Unit,
    onSelectIconPack: (String?) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    val scrollState = rememberScrollState()

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

        // ── Divider ──────────────────────────────────────
        HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

        // ── Categories ───────────────────────────────────
        SectionLabel("Categorías  •  toca el ícono o el nombre para editar")

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            categoryConfigs.forEach { config ->
                CategorySettingsRow(
                    config = config,
                    onRename = { onCategoryRename(config.category, it) },
                    onIconChange = { onCategoryIconChange(config.category, it) },
                    onToggleHidden = { onCategoryToggleHidden(config.category) },
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

        // ── Reset ─────────────────────────────────────────
        var showResetConfirm by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
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
    onRename: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onToggleHidden: () -> Unit,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    var showIconPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

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

            // ── Visibility toggle ─────────────────────────────
            IconButton(
                onClick = onToggleHidden,
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
