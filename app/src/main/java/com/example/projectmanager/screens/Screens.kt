package com.example.projectmanager.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectmanager.termux.TermuxManager
import com.example.projectmanager.services.*
import com.example.projectmanager.models.*
import com.example.projectmanager.utils.PermissionsHelper
import com.example.projectmanager.utils.TermuxInstaller
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxScreen() {
    val context = LocalContext.current
    val termuxManager = remember { TermuxManager(context) }
    val scope = rememberCoroutineScope()

    var hasPermissions by remember { mutableStateOf(PermissionsHelper.hasAllPermissions(context)) }
    var isTermuxInstalled by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }
    var showQuickCommands by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isTermuxInstalled = termuxManager.isTermuxInstalled()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermissions = PermissionsHelper.hasStoragePermission(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!hasPermissions) {
            PermissionsSection(
                onRequestPermissions = { permissionLauncher.launch(PermissionsHelper.REQUIRED_PERMISSIONS) },
                onRequestStoragePermission = { PermissionsHelper.getStoragePermissionIntent(context)?.let { storagePermissionLauncher.launch(it) } }
            )
            return@Column
        }

        if (!isTermuxInstalled) {
            TermuxInstallSection()
            return@Column
        }

        Text("Terminal Termux", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (showQuickCommands) {
            QuickCommandsSection(
                onCommandSelected = { commandInput = it },
                onDismiss = { showQuickCommands = false }
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            TextButton(onClick = { showQuickCommands = true }) {
                Icon(Icons.Default.Apps, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Afficher les commandes rapides")
            }
        }

        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                items(commandHistory) { (command, output) ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "$ ", color = Color(0xFF4CAF50), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                            Text(text = command, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        }
                        if (output.isNotEmpty()) {
                            Text(text = output, color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Entrez une commande...") },
                singleLine = true,
                enabled = !isExecuting
            )
            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        scope.launch {
                            isExecuting = true
                            val result = termuxManager.executeCommand(commandInput)
                            val output = if (result.success) result.output.ifEmpty { "Succes" } else "Erreur: ${result.error}"
                            commandHistory = commandHistory + (commandInput to output)
                            commandInput = ""
                            isExecuting = false
                        }
                    }
                },
                enabled = commandInput.isNotBlank() && !isExecuting
            ) {
                if (isExecuting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, null)
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val monitoringManager = remember { MonitoringManager(context) }
    val backupManager = remember { BackupManager(context) }
    val scope = rememberCoroutineScope()

    val systemStats by monitoringManager.systemStats.collectAsState()
    var servicesStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var backups by remember { mutableStateOf<List<BackupInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            monitoringManager.getSystemStats()
            servicesStatus = monitoringManager.checkAllServices()
            backups = backupManager.listBackups()
            kotlinx.coroutines.delay(5000)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ProjectManager Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Uptime: ${systemStats.uptime}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("CPU", "${systemStats.cpuUsage.toInt()}%", systemStats.cpuUsage / 100f, Modifier.weight(1f))
                StatCard("RAM", "${systemStats.memoryUsage.percentage.toInt()}%", systemStats.memoryUsage.percentage / 100f, Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    servicesStatus.forEach { (name, isRunning) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name)
                            Text(if (isRunning) "Actif" else "Inactif", color = if (isRunning) Color(0xFF4CAF50) else Color.Gray)
                        }
                    }
                }
            }
        }

        item { Text("Backups recents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

        items(backups.take(3)) { backup: BackupInfo ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(backup.name, fontWeight = FontWeight.Bold)
                        Text("${backup.size} - ${backup.date}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { scope.launch { backupManager.restoreBackup(backup.path) } }) {
                        Icon(Icons.Default.Restore, null)
                    }
                }
            }
        }
    }
}

@Composable
fun MyTemplatesScreen() {
    val context = LocalContext.current
    val templateManager = remember { TemplateManager(context) }
    val scope = rememberCoroutineScope()
    val templates by templateManager.templates.collectAsState()
    val categories by templateManager.categories.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { templateManager.loadTemplates() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Mes Templates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, null)
                Text("Creer")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates) { template ->
                TemplateCard(template = template, onDeploy = {}, onExport = {}, onDelete = { scope.launch { templateManager.deleteTemplate(template.name) } })
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(categories = categories, onDismiss = { showCreateDialog = false }, onCreate = { n, s, c, d -> scope.launch { templateManager.createTemplate(n, s, c, d) } })
    }
}

@Composable
fun DeploymentsScreen() {
    val context = LocalContext.current
    val deploymentManager = remember { DeploymentManager(context) }
    val scope = rememberCoroutineScope()
    val deployments by deploymentManager.deployments.collectAsState()

    LaunchedEffect(Unit) { deploymentManager.loadDeployments() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mes Deploiements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(deployments) { deployment ->
                DeploymentCard(deployment = deployment, onStart = { scope.launch { deploymentManager.startDeployment(deployment.name, deployment.webServer) } }, onStop = { scope.launch { deploymentManager.stopDeployment(deployment.name, deployment.webServer) } }, onBackup = {}, onDelete = { scope.launch { deploymentManager.deleteDeployment(deployment.name) } })
            }
        }
    }
}

@Composable
fun SSHTerminalScreen() {
    val context = LocalContext.current
    val sshManager = remember { SSHManager(context) }
    val scope = rememberCoroutineScope()
    var commandInput by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SSH Terminal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                item { Text(terminalOutput, color = Color.Green, fontFamily = FontFamily.Monospace) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = commandInput, onValueChange = { commandInput = it }, modifier = Modifier.weight(1f), placeholder = { Text("Commande SSH...") })
            IconButton(onClick = {
                scope.launch {
                    val conn = SSHConnection("SSH Terminal", "localhost", 22, "user")
                    val result = sshManager.executeSSHCommand(conn, commandInput)
                    terminalOutput += "\n$ ${commandInput}\n${result.output}${result.error}"
                    commandInput = ""
                }
            }) { Icon(Icons.AutoMirrored.Filled.Send, null) }
        }
    }
}

@Composable private fun StatCard(label: String, value: String, progress: Float, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}

@Composable private fun PermissionsSection(onRequestPermissions: () -> Unit, onRequestStoragePermission: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Security, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Permissions requises", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) { Text("Accorder les permissions") }
        OutlinedButton(onClick = onRequestStoragePermission, modifier = Modifier.fillMaxWidth()) { Text("Gerer tous les fichiers") }
    }
}

@Composable private fun TermuxInstallSection() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Android, null, modifier = Modifier.size(64.dp))
        Text("Termux non installe", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = {
            val installer = TermuxInstaller(context)
            installer.installTermux()
        }) {
            Text("Installer Termux")
        }
    }
}

@Composable private fun QuickCommandsSection(onCommandSelected: (String) -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Commandes rapides", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onCommandSelected("pkg update") }) { Text("Update") }
                Button(onClick = { onCommandSelected("pkg upgrade") }) { Text("Upgrade") }
            }
        }
    }
}

@Composable fun ApacheScreen() { Box(Modifier.fillMaxSize()) { Text("Apache Screen") } }
@Composable fun NginxScreen() { Box(Modifier.fillMaxSize()) { Text("Nginx Screen") } }
@Composable fun PHPScreen() { Box(Modifier.fillMaxSize()) { Text("PHP Screen") } }
@Composable fun PostgreSQLScreen() { Box(Modifier.fillMaxSize()) { Text("PostgreSQL Screen") } }
@Composable fun MySQLScreen() { Box(Modifier.fillMaxSize()) { Text("MySQL Screen") } }
@Composable fun StrapiScreen() { Box(Modifier.fillMaxSize()) { Text("Strapi Screen") } }
