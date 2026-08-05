package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.UserSettings
import com.example.ui.theme.TerracottaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelDialog(
    settings: UserSettings,
    onUpdateAdminMode: (Boolean) -> Unit,
    onUpdateTemperature: (Float) -> Unit,
    onUpdateSystemPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var promptInput by remember { mutableStateOf(settings.systemPrompt) }
    var tempVal by remember { mutableFloatStateOf(settings.temperature) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("admin_panel_dialog")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TerracottaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Admin Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Admin Mode Active", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Enforce rate limits & system prompts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.isAdminMode,
                        onCheckedChange = onUpdateAdminMode
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // System Instruction Prompt
                Text("System Prompt Instruction", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5,
                    placeholder = { Text("Define AI assistant persona...") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature Slider
                Text("Temperature (${String.format("%.2f", tempVal)})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Higher values make output more creative, lower values more deterministic.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = tempVal,
                    onValueChange = { tempVal = it },
                    valueRange = 0.0f..1.0f,
                    steps = 10
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Security Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Security & Backend Status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• API Key Protection: Injected via BuildConfig", fontSize = 11.sp)
                        Text("• Enforced SSL HTTPS Encrypted Tunnel", fontSize = 11.sp)
                        Text("• Rate Limit: 60 requests/minute (Active)", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onUpdateSystemPrompt(promptInput)
                        onUpdateTemperature(tempVal)
                        Toast.makeText(context, "Admin settings updated", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Configuration", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
