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

// Composants pour Apache

@Composable
fun VirtualHostsTab(
    virtualHosts: List<VirtualHost>,
    onEnable: (VirtualHost) -> Unit,
    onDisable: (VirtualHost) -> Unit,
    onAdd: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Virtual Hosts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAdd, enabled = !isLoading) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(virtualHosts) { vhost ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vhost.serverName, fontWeight = FontWeight.Bold)
                            Text("Port: ${vhost.port}", style = MaterialTheme.typography.bodySmall)
                            Text(vhost.documentRoot, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = vhost.enabled,
                            onCheckedChange = { if (it) onEnable(vhost) else onDisable(vhost) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateVirtualHostDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var docRoot by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Creer Virtual Host") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = docRoot,
                    onValueChange = { docRoot = it },
                    label = { Text("Document Root") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(serverName, docRoot, port.toIntOrNull() ?: 8080)
                    onDismiss()
                },
                enabled = serverName.isNotBlank() && docRoot.isNotBlank()
            ) {
                Text("Creer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// Composants pour Nginx

@Composable
fun ServerBlocksTab(
    serverBlocks: List<ServerBlock>,
    onEnable: (ServerBlock) -> Unit,
    onDisable: (ServerBlock) -> Unit,
    onAdd: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Server Blocks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAdd, enabled = !isLoading) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(serverBlocks) { block ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(block.serverName, fontWeight = FontWeight.Bold)
                            Text("Port: ${block.port}", style = MaterialTheme.typography.bodySmall)
                            Text(block.root, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = block.enabled,
                            onCheckedChange = { if (it) onEnable(block) else onDisable(block) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateServerBlockDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, Boolean, String) -> Unit
) {
    var serverName by remember { mutableStateOf("") }
    var root by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var isProxy by remember { mutableStateOf(false) }
    var proxyPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Creer Server Block") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Server Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = root,
                    onValueChange = { root = it },
                    label = { Text("Root") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isProxy, onCheckedChange = { isProxy = it })
                    Text("Proxy inverse")
                }
                if (isProxy) {
                    OutlinedTextField(
                        value = proxyPass,
                        onValueChange = { proxyPass = it },
                        label = { Text("Proxy Pass") },
                        placeholder = { Text("http://localhost:3000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(serverName, root, port.toIntOrNull() ?: 8080, isProxy, proxyPass)
                    onDismiss()
                },
                enabled = serverName.isNotBlank()
            ) {
                Text("Creer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

