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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.projectmanager.models.*

// Composants utilitaires reutilisables pour les ecrans de services

@Composable
fun ServiceHeader(
    serviceName: String,
    status: ServiceStatus,
    version: String?,
    port: Int,
    isInstalled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ServiceStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                ServiceStatus.STOPPED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isInstalled) {
                    version?.let {
                        Text("Version: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Port: $port", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isInstalled) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (status) {
                        ServiceStatus.RUNNING -> Color(0xFF4CAF50)
                        ServiceStatus.STOPPED -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    }
                ) {
                    Text(
                        text = when (status) {
                            ServiceStatus.RUNNING -> "Running"
                            ServiceStatus.STOPPED -> "Stopped"
                            else -> "Unknown"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InstallationSection(
    serviceName: String,
    onInstall: () -> Unit,
    isLoading: Boolean,
    statusMessage: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "$serviceName n'est pas installe",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Cliquez pour installer $serviceName via Termux",
                style = MaterialTheme.typography.bodySmall
            )
            
            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    statusMessage,
                    color = if (statusMessage.contains("Erreur")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onInstall,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Download, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Installer $serviceName")
            }
        }
    }
}

@Composable
fun ServiceControlTab(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onTest: () -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Controles du service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start")
            }
            
            Button(
                onClick = onStop,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRestart,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restart")
            }
            
            OutlinedButton(
                onClick = onTest,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test")
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

@Composable
fun LogsTab(
    logs: List<String>,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(logs) { log ->
                    Text(
                        log,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
