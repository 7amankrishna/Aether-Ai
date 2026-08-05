package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AppThemeMode
import com.example.data.local.UserSettings
import com.example.domain.models.ProviderRegistry
import com.example.ui.theme.TerracottaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: UserSettings,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateProviderAndModel: (String, String) -> Unit,
    onUpdateCustomApiKey: (String) -> Unit,
    onUpdateCustomEndpoint: (String) -> Unit,
    onFetchRemoteModels: (String, String) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateStreaming: (Boolean) -> Unit,
    onUpdateHaptics: (Boolean) -> Unit,
    onClearAllHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val providers by ProviderRegistry.providersState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Theme Mode Section
                Text("Appearance Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onUpdateTheme(mode) },
                            label = { Text(mode.name.lowercase().capitalize(), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Endpoint & Key Section
                Text("Aerolink / Provider API Credentials", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Input your Aerolink API key or custom URL to automatically load and select remote models.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = settings.customEndpointUrl,
                    onValueChange = onUpdateCustomEndpoint,
                    label = { Text("Base API Endpoint URL", fontSize = 12.sp) },
                    placeholder = { Text("https://capi.aerolink.lat/") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = settings.customApiKey,
                    onValueChange = onUpdateCustomApiKey,
                    label = { Text("API Key (e.g. Aerolink Key)", fontSize = 12.sp) },
                    placeholder = { Text("Enter your Aerolink or Custom API key") },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onFetchRemoteModels(settings.customEndpointUrl, settings.customApiKey)
                        Toast.makeText(context, "Fetching models from endpoint...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Fetch & Sync Models", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Provider & Model Section
                Text("AI Provider & Model Selection", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                providers.forEach { provider ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (settings.selectedProviderId == provider.id)
                                TerracottaPrimary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            provider.models.forEach { model ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.selectedProviderId == provider.id && settings.selectedModelId == model.id,
                                        onClick = { onUpdateProviderAndModel(provider.id, model.id) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(model.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(model.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Font Size Slider
                Text("Font Size (${settings.fontSizeSp.toInt()} sp)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = onUpdateFontSize,
                    valueRange = 12f..20f,
                    steps = 7
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Streaming Responses", fontSize = 14.sp)
                    Switch(checked = settings.isStreamingEnabled, onCheckedChange = onUpdateStreaming)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Feedback", fontSize = 14.sp)
                    Switch(checked = settings.isHapticsEnabled, onCheckedChange = onUpdateHaptics)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Conversations exported to JSON", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onClearAllHistory()
                            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}

