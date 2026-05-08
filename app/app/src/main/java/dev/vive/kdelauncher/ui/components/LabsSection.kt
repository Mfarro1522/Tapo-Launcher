package dev.vive.kdelauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vive.kdelauncher.data.model.AiProviderType
import dev.vive.kdelauncher.data.model.AppCategorization
import dev.vive.kdelauncher.ui.AiConnectionState
import dev.vive.kdelauncher.ui.OrganizationState
import dev.vive.kdelauncher.ui.theme.LauncherTypography
import dev.vive.kdelauncher.ui.theme.LocalColors
import dev.vive.kdelauncher.ui.theme.LocalLauncherAccent
import kotlinx.coroutines.delay

@Composable
fun LabsSection(
    labsEnabled: Boolean,
    onToggleLabs: (Boolean) -> Unit,
    aiProvider: AiProviderType,
    aiConnectionState: AiConnectionState,
    aiModel: String,
    organizationState: OrganizationState,
    onConnectAi: (AiProviderType, String) -> Unit,
    onDisconnectAi: () -> Unit,
    onSetAiModel: (String) -> Unit,
    onOrganizeApps: () -> Unit,
    onApplySuggestions: (List<AppCategorization>) -> Unit,
    onCancelOrganization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current

    var showConsentDialog by remember { mutableStateOf(false) }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Permiso de Privacidad") },
            text = {
                Text(
                    "Para usar TAPO Labs y organizar tus aplicaciones, necesitamos tu permiso para enviar la lista de tus apps instaladas a un proveedor de Inteligencia Artificial (Groq, Gemini u OpenRouter). Tus datos no se almacenan permanentemente.\n\n¿Aceptas activar esta función?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onToggleLabs(true)
                    showConsentDialog = false
                }) {
                    Text("Aceptar", color = accent.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Cancelar", color = colors.onSurfaceVariant)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f))
                .clickable {
                    if (!labsEnabled) {
                        showConsentDialog = true
                    } else {
                        onToggleLabs(false)
                    }
                }
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
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Science,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF8B5CF6)
                    )
                }
                Column {
                    Text(
                        text = "TAPO Labs",
                        style = LauncherTypography.bodyMedium,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Funciones experimentales con IA",
                        style = LauncherTypography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = labsEnabled,
                onCheckedChange = {
                    if (it) {
                        showConsentDialog = true
                    } else {
                        onToggleLabs(false)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent.primary,
                    checkedTrackColor = accent.primaryBg,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                )
            )
        }

        AnimatedVisibility(
            visible = labsEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Experimental. Los LLMs pueden cometer errores. Tus datos de apps no se comparten permanentemente, solo se usan en el prompt.",
                        style = LauncherTypography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.onBackground.copy(alpha = 0.8f)
                    )
                }

                if (aiConnectionState is AiConnectionState.Connected) {
                    ConnectedLabsContent(
                        aiProvider = aiProvider,
                        aiModel = aiModel,
                        aiConnectionState = aiConnectionState,
                        organizationState = organizationState,
                        onDisconnectAi = onDisconnectAi,
                        onSetAiModel = onSetAiModel,
                        onOrganizeApps = onOrganizeApps,
                        onApplySuggestions = onApplySuggestions,
                        onCancelOrganization = onCancelOrganization
                    )
                } else {
                    DisconnectedLabsContent(
                        aiProvider = aiProvider,
                        aiConnectionState = aiConnectionState,
                        onConnectAi = onConnectAi
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedLabsContent(
    aiProvider: AiProviderType,
    aiModel: String,
    aiConnectionState: AiConnectionState.Connected,
    organizationState: OrganizationState,
    onDisconnectAi: () -> Unit,
    onSetAiModel: (String) -> Unit,
    onOrganizeApps: () -> Unit,
    onApplySuggestions: (List<AppCategorization>) -> Unit,
    onCancelOrganization: () -> Unit,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current
    val models = aiConnectionState.models
    var modelExpanded by remember(models) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Conectado a ${aiProvider.displayName}",
                style = LauncherTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF10B981)
            )
            Text(
                text = if (aiModel.isNotBlank()) "Modelo activo: $aiModel" else "Selecciona un modelo para continuar",
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        TextButton(onClick = onDisconnectAi) {
            Text("Desconectar", color = Color(0xFFEF4444))
        }
    }

    if (models.isNotEmpty()) {
        LabsSelectorField(
            label = "Modelo",
            value = if (aiModel.isNotBlank()) aiModel else "Selecciona un modelo",
            expanded = modelExpanded,
            supportingText = "${models.size} modelos disponibles",
            onClick = { modelExpanded = !modelExpanded }
        )

        AnimatedVisibility(visible = modelExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                models.forEach { model ->
                    LabsOptionCard(
                        title = model,
                        subtitle = null,
                        selected = model == aiModel,
                        onClick = {
                            onSetAiModel(model)
                            modelExpanded = false
                        }
                    )
                }
            }
        }
    }

    when (organizationState) {
        is OrganizationState.Idle -> {
            Button(
                onClick = onOrganizeApps,
                modifier = Modifier.fillMaxWidth(),
                enabled = aiModel.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.primaryBg,
                    contentColor = accent.primary
                )
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Organizar apps automáticamente")
            }
        }

        is OrganizationState.Loading -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accent.primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    "Analizando apps…",
                    style = LauncherTypography.bodyMedium,
                    color = colors.onBackground
                )
            }
        }

        is OrganizationState.Preview -> {
            var debugExpanded by rememberSaveable { mutableStateOf(false) }
            val suggestions = buildList {
                addAll(organizationState.result.fromCache)
                addAll(organizationState.result.fromLocal)
                addAll(organizationState.result.fromAi)
                addAll(organizationState.result.fromFallback)
            }

            Text(
                text = "Sugerencias encontradas: ${suggestions.size}",
                style = LauncherTypography.titleSmall,
                color = colors.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip("Caché", organizationState.result.fromCache.size)
                SuggestionChip("Local", organizationState.result.fromLocal.size)
                SuggestionChip("IA", organizationState.result.fromAi.size)
                SuggestionChip("Fallback", organizationState.result.fromFallback.size)
            }

            LabsSelectorField(
                label = "Modo debug",
                value = if (debugExpanded) "Ocultar diagnóstico" else "Ver diagnóstico",
                expanded = debugExpanded,
                supportingText = "${organizationState.result.debug.providerName} · ${organizationState.result.debug.modelId}",
                onClick = { debugExpanded = !debugExpanded }
            )

            AnimatedVisibility(visible = debugExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.28f))
                        .border(1.dp, colors.border.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = accent.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Diagnóstico de ejecución",
                            style = LauncherTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onBackground
                        )
                    }

                    Text(
                        text = "Proveedor: ${organizationState.result.debug.providerName}\nModelo: ${organizationState.result.debug.modelId}\nApps candidatas: ${organizationState.result.debug.candidateCount}\nCoincidencias locales: ${organizationState.result.debug.localMatchCount}\nApps enviadas a IA: ${organizationState.result.debug.aiCandidateCount}\nLotes: ${organizationState.result.debug.totalBatches}",
                        style = LauncherTypography.bodySmall,
                        color = colors.onBackground.copy(alpha = 0.92f)
                    )

                    if (organizationState.result.debug.batchReports.isEmpty()) {
                        Text(
                            text = "No hubo lotes enviados a IA.",
                            style = LauncherTypography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            organizationState.result.debug.batchReports.forEach { batch ->
                                LabsOptionCard(
                                    title = "Lote ${batch.batchIndex} · ${batch.status}",
                                    subtitle = "${batch.appCount} apps · ${batch.detail}",
                                    selected = batch.status == "AI_OK",
                                    onClick = { },
                                    subtitleMaxLines = 6
                                )
                            }
                        }
                    }
                }
            }

            if (suggestions.isEmpty()) {
                Text(
                    text = "No hay apps candidatas para reorganizar en este momento.",
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.take(8).forEach { suggestion ->
                        LabsOptionCard(
                            title = suggestion.packageName.substringAfterLast('.'),
                            subtitle = "${suggestion.categoryId} · ${suggestion.source.name}",
                            selected = false,
                            onClick = { }
                        )
                    }

                    if (suggestions.size > 8) {
                        Text(
                            text = "… y ${suggestions.size - 8} más",
                            style = LauncherTypography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelOrganization,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onApplySuggestions(suggestions) },
                    modifier = Modifier.weight(1f),
                    enabled = suggestions.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.primary)
                ) {
                    Text("Aplicar")
                }
            }
        }

        is OrganizationState.Applied -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF10B981))
                Spacer(Modifier.size(8.dp))
                Text("Categorías aplicadas", color = Color(0xFF10B981))
            }
            LaunchedEffect(Unit) {
                delay(2000)
                onCancelOrganization()
            }
        }
    }
}

@Composable
private fun DisconnectedLabsContent(
    aiProvider: AiProviderType,
    aiConnectionState: AiConnectionState,
    onConnectAi: (AiProviderType, String) -> Unit,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current

    var selectedProvider by remember(aiProvider) { mutableStateOf(aiProvider) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var providerExpanded by remember { mutableStateOf(false) }

    LabsSelectorField(
        label = "Proveedor IA",
        value = selectedProvider.displayName,
        expanded = providerExpanded,
        supportingText = selectedProvider.limitDescription,
        onClick = { providerExpanded = !providerExpanded }
    )

    AnimatedVisibility(visible = providerExpanded) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AiProviderType.entries.forEach { provider ->
                LabsOptionCard(
                    title = provider.displayName,
                    subtitle = provider.limitDescription,
                    selected = provider == selectedProvider,
                    onClick = {
                        selectedProvider = provider
                        providerExpanded = false
                    }
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.primaryBg),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = accent.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Documentación de API",
                style = LauncherTypography.bodyMedium,
                color = colors.onBackground
            )
            Text(
                text = selectedProvider.docsUrl,
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        label = { Text("API Key") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        supportingText = {
            Text(
                text = "Tu clave se usa para consultar modelos y clasificar apps con ${selectedProvider.displayName}.",
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    )

    if (aiConnectionState is AiConnectionState.Error) {
        Text(
            text = "Error: ${aiConnectionState.message}",
            style = LauncherTypography.bodySmall,
            color = Color(0xFFEF4444)
        )
    }

    Button(
        onClick = { onConnectAi(selectedProvider, apiKey) },
        modifier = Modifier.fillMaxWidth(),
        enabled = apiKey.isNotBlank() && aiConnectionState !is AiConnectionState.Loading,
        colors = ButtonDefaults.buttonColors(containerColor = accent.primary)
    ) {
        if (aiConnectionState is AiConnectionState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text("Conectar")
        }
    }
}

@Composable
private fun LabsSelectorField(
    label: String,
    value: String,
    expanded: Boolean,
    supportingText: String,
    onClick: () -> Unit,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = LauncherTypography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.85f)
            )
            Text(
                text = value,
                style = LauncherTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supportingText,
                style = LauncherTypography.bodySmall.copy(fontSize = 11.sp),
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (expanded) accent.primaryBg else colors.surface),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = if (expanded) accent.primary else colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabsOptionCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    subtitleMaxLines: Int = 1,
) {
    val colors = LocalColors.current
    val accent = LocalLauncherAccent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.primaryBg else colors.surfaceVariant.copy(alpha = 0.32f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent.primary else colors.border.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LauncherTypography.bodyMedium,
                color = if (selected) accent.primary else colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = LauncherTypography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = subtitleMaxLines,
                    overflow = if (subtitleMaxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.primary),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, count: Int) {
    val colors = LocalColors.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label: $count",
            style = LauncherTypography.bodySmall,
            color = colors.onBackground
        )
    }
}
