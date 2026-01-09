package com.example.projectmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.projectmanager.models.*

@Composable
fun PHPServiceControlTab(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onTest: () -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    ServiceControlTab(
        onStart = onStart,
        onStop = onStop,
        onRestart = onRestart,
        onTest = onTest,
        isLoading = isLoading,
        statusMessage = statusMessage
    )
}

@Composable
fun PHPExtensionsTab(
    extensions: List<PHPExtension>,
    onInstall: (PHPExtension) -> Unit,
    onUninstall: (PHPExtension) -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Extensions PHP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(extensions) { ext ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ext.displayName, fontWeight = FontWeight.Bold)
                            Text(ext.description, style = MaterialTheme.typography.bodySmall)
                        }
                        if (ext.isInstalled) {
                            Button(
                                onClick = { onUninstall(ext) },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Desinstaller")
                            }
                        } else {
                            Button(
                                onClick = { onInstall(ext) },
                                enabled = !isLoading
                            ) {
                                Text("Installer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PHPConfigTab(
    configPath: String,
    onEdit: () -> Unit,
    onReload: () -> Unit,
    onUpdateDirective: (String, String) -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Configuration PHP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = padding(16.dp)) { // Should be Modifier.padding
                Text("Fichier de configuration", fontWeight = FontWeight.Bold)
                Text(configPath, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEdit,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editer")
            }

            Button(
                onClick = onReload,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reload")
            }
        }

        if (statusMessage.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (statusMessage.contains("Erreur"))
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    statusMessage,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
