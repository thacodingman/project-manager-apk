package com.example.projectmanager.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.net.toUri
import com.example.projectmanager.termux.TermuxManager
import com.example.projectmanager.services.*
import com.example.projectmanager.models.*
import com.example.projectmanager.utils.PermissionsHelper
import com.example.projectmanager.utils.TermuxInstaller
import kotlinx.coroutines.launch

// Extension functions to map manager methods to screen method names
private suspend fun ApacheManager.isApacheInstalled() = checkInstallation().success
private suspend fun ApacheManager.isApacheRunning() = checkStatus().success
private suspend fun ApacheManager.getApacheVersion() = getVersion().output
private suspend fun ApacheManager.installApache() = install()
private suspend fun ApacheManager.startApache() = start()
private suspend fun ApacheManager.stopApache() = stop()
private suspend fun ApacheManager.getApacheLogs() = getErrorLogs()
private suspend fun ApacheManager.getApacheConfig() = testConfig()

private suspend fun NginxManager.isNginxInstalled() = checkInstallation().success
private suspend fun NginxManager.isNginxRunning() = checkStatus().success
private suspend fun NginxManager.getNginxVersion() = getVersion().output
private suspend fun NginxManager.installNginx() = install()
private suspend fun NginxManager.startNginx() = start()
private suspend fun NginxManager.stopNginx() = stop()
private suspend fun NginxManager.reloadNginx() = restart()
private suspend fun NginxManager.getNginxLogs() = getErrorLogs()

private suspend fun PHPManager.isPHPInstalled() = checkInstallation().success
private suspend fun PHPManager.getPHPVersion() = getVersion().output
private suspend fun PHPManager.installPHP() = install()

private suspend fun PostgreSQLManager.isPostgreSQLInstalled() = checkInstallation().success
private suspend fun PostgreSQLManager.isPostgreSQLRunning() = checkStatus().success
private suspend fun PostgreSQLManager.installPostgreSQL() = install()
private suspend fun PostgreSQLManager.startPostgreSQL() = start()
private suspend fun PostgreSQLManager.stopPostgreSQL() = stop()

private suspend fun MySQLManager.isMySQLInstalled() = checkInstallation().success
private suspend fun MySQLManager.isMySQLRunning() = checkStatus().success
private suspend fun MySQLManager.installMySQL() = install()
private suspend fun MySQLManager.startMySQL() = start()
private suspend fun MySQLManager.stopMySQL() = stop()

// Extension functions end here

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxScreen() {
    val context = LocalContext.current
    val termuxManager = remember { TermuxManager(context) }
    val termuxInstaller = remember { TermuxInstaller(context) }
    val scope = rememberCoroutineScope()

    var hasPermissions by remember { mutableStateOf(PermissionsHelper.hasAllPermissions(context)) }
    var isTermuxInstalled by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }
    var showQuickCommands by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isTermuxInstalled = termuxInstaller.isTermuxInstalled()
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
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
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
        CreateTemplateDialog(
            categories = categories,
            onDismiss = { showCreateDialog = false },
            onCreate = { n, s, c, d ->
                scope.launch {
                    templateManager.createTemplate(n, s, c, d)
                }
                showCreateDialog = false
            }
        )
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
    val installer = remember { TermuxInstaller(context) }

    var isChecking by remember { mutableStateOf(true) }
    var termuxVersion by remember { mutableStateOf<String?>(null) }
    var isApkAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        termuxVersion = installer.getTermuxVersion()
        isApkAvailable = installer.isTermuxApkAvailable()
        isChecking = false
    }

    if (isChecking) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Checking Termux installation...")
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Android,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (termuxVersion != null) Color(0xFF4CAF50) else Color(0xFFFF5722)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (termuxVersion != null) {
            // Termux is already installed
            Text(
                "Termux Already Installed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version:", fontWeight = FontWeight.Bold)
                        Text(termuxVersion ?: "Unknown", color = Color(0xFF4CAF50))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Package:", fontWeight = FontWeight.Bold)
                        Text("com.termux", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status:", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ready", color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Please grant storage permissions to use Termux features",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray
            )

        } else {
            // Termux is NOT installed
            Text(
                "Termux Not Installed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isApkAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Termux APK Included",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            installer.getIncludedApkInfo(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { installer.installTermux() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install Termux from Included APK", color = Color.Black)
                }

            } else {
                Text(
                    "Termux APK not found in app assets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://f-droid.org/packages/com.termux/".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download from F-Droid")
                }
            }
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

@Composable
fun ApacheScreen() {
    val context = LocalContext.current
    val apacheManager = remember { ApacheManager(context) }
    val scope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var apacheVersion by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf("Loading logs...") }
    var vhosts by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddVHostDialog by remember { mutableStateOf(false) }
    var newVHostName by remember { mutableStateOf("") }
    var newVHostPort by remember { mutableStateOf("8080") }
    var newVHostRoot by remember { mutableStateOf("/data/data/com.termux/files/home/www") }

    LaunchedEffect(Unit) {
        isLoading = true
        isInstalled = apacheManager.isApacheInstalled()
        if (isInstalled) {
            isRunning = apacheManager.isApacheRunning()
            apacheVersion = apacheManager.getApacheVersion()
            // Load virtual hosts
            scope.launch {
                val result = apacheManager.listVirtualHosts()
                vhosts = result.output.lines().filter { it.isNotBlank() }
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Text(
            "Apache HTTP Server",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF39FF14))
            }
            return@Column
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isRunning) "Running" else if (isInstalled) "Stopped" else "Not Installed",
                                color = if (isRunning) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }

                    if (isInstalled && apacheVersion.isNotEmpty()) {
                        Text("Version: $apacheVersion", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isInstalled) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    apacheManager.installApache()
                                    isInstalled = apacheManager.isApacheInstalled()
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install Apache", color = Color.Black)
                        }
                    } else {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        apacheManager.startApache()
                                        isRunning = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start")
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        apacheManager.stopApache()
                                        isRunning = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    apacheManager.stopApache()
                                    kotlinx.coroutines.delay(500)
                                    apacheManager.startApache()
                                    isRunning = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restart")
                        }
                    }
                }
            }
        }

        if (isInstalled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFF39FF14)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("VirtualHosts", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Configuration", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = {
                    selectedTab = 2
                    scope.launch {
                        val result = apacheManager.getApacheLogs()
                        logs = result.output.ifEmpty { "No logs available" }
                    }
                }) {
                    Text("Logs", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // VirtualHosts Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("VirtualHosts Management", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddVHostDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add VHost", color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (vhosts.isEmpty()) {
                                Text(
                                    "No virtual hosts configured",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                LazyColumn {
                                    items(vhosts) { vhost ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(vhost, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "Port: 8080",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Row {
                                                    IconButton(onClick = {
                                                        scope.launch {
                                                            apacheManager.enableVirtualHost(vhost)
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            null,
                                                            tint = Color(0xFF4CAF50)
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        scope.launch {
                                                            apacheManager.disableVirtualHost(vhost)
                                                            val result = apacheManager.listVirtualHosts()
                                                            vhosts = result.output.lines().filter { it.isNotBlank() }
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            null,
                                                            tint = Color(0xFFFF5722)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Configuration Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("httpd.conf", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        scope.launch {
                                            apacheManager.getApacheConfig()
                                            // TODO: Show config editor
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Text("Edit Config", color = Color.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Apache configuration file editor",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                2 -> {
                    // Logs Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item {
                                Text(
                                    logs,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add VirtualHost Dialog
    if (showAddVHostDialog) {
        AlertDialog(
            onDismissRequest = { showAddVHostDialog = false },
            title = { Text("Add Apache VirtualHost") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newVHostName,
                        onValueChange = { newVHostName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("example.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newVHostPort,
                        onValueChange = { newVHostPort = it },
                        label = { Text("Port") },
                        placeholder = { Text("8080") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newVHostRoot,
                        onValueChange = { newVHostRoot = it },
                        label = { Text("Document Root") },
                        placeholder = { Text("/data/data/com.termux/files/home/www") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            apacheManager.createVirtualHost(
                                serverName = newVHostName,
                                port = newVHostPort.toIntOrNull() ?: 8080,
                                documentRoot = newVHostRoot
                            )
                            showAddVHostDialog = false
                            newVHostName = ""
                            newVHostPort = "8080"
                            newVHostRoot = "/data/data/com.termux/files/home/www"
                            // Reload vhosts list
                            val result = apacheManager.listVirtualHosts()
                            vhosts = result.output.lines().filter { it.isNotBlank() }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVHostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun NginxScreen() {
    val context = LocalContext.current
    val nginxManager = remember { NginxManager(context) }
    val scope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var nginxVersion by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf("Loading logs...") }
    var serverBlocks by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddServerBlockDialog by remember { mutableStateOf(false) }
    var newServerName by remember { mutableStateOf("") }
    var newServerPort by remember { mutableStateOf("8081") }
    var newServerRoot by remember { mutableStateOf("/data/data/com.termux/files/home/www") }

    LaunchedEffect(Unit) {
        isLoading = true
        isInstalled = nginxManager.isNginxInstalled()
        if (isInstalled) {
            isRunning = nginxManager.isNginxRunning()
            nginxVersion = nginxManager.getNginxVersion()
            // Load server blocks
            scope.launch {
                val result = nginxManager.listServerBlocks()
                serverBlocks = result.output.lines().filter { it.isNotBlank() }
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Nginx Web Server",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF39FF14))
            }
            return@Column
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isRunning) "Running" else if (isInstalled) "Stopped" else "Not Installed",
                                color = if (isRunning) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }

                    if (isInstalled && nginxVersion.isNotEmpty()) {
                        Text("Version: $nginxVersion", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isInstalled) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    nginxManager.installNginx()
                                    isInstalled = nginxManager.isNginxInstalled()
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install Nginx", color = Color.Black)
                        }
                    } else {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        nginxManager.startNginx()
                                        isRunning = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start")
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        nginxManager.stopNginx()
                                        isRunning = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    nginxManager.reloadNginx()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reload")
                        }
                    }
                }
            }
        }

        if (isInstalled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFF39FF14)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Server Blocks", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("SSL/TLS", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = {
                    selectedTab = 2
                    scope.launch {
                        val result = nginxManager.getNginxLogs()
                        logs = result.output.ifEmpty { "No logs available" }
                    }
                }) {
                    Text("Logs", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Server Blocks Management", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddServerBlockDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Server Block", color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (serverBlocks.isEmpty()) {
                                Text(
                                    "No server blocks configured",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                LazyColumn {
                                    items(serverBlocks) { block ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(block, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "Port: 8081",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Row {
                                                    IconButton(onClick = {
                                                        scope.launch {
                                                            nginxManager.enableServerBlock(block)
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            null,
                                                            tint = Color(0xFF4CAF50)
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        scope.launch {
                                                            nginxManager.disableServerBlock(block)
                                                            val result = nginxManager.listServerBlocks()
                                                            serverBlocks = result.output.lines().filter { it.isNotBlank() }
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            null,
                                                            tint = Color(0xFFFF5722)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SSL/TLS Certificates", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Manage SSL certificates for HTTPS",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { /* TODO: Generate SSL */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate Certificate", color = Color.Black)
                            }
                        }
                    }
                }
                2 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item {
                                Text(
                                    logs,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Server Block Dialog
    if (showAddServerBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddServerBlockDialog = false },
            title = { Text("Add Nginx Server Block") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newServerName,
                        onValueChange = { newServerName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("example.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newServerPort,
                        onValueChange = { newServerPort = it },
                        label = { Text("Port") },
                        placeholder = { Text("8081") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newServerRoot,
                        onValueChange = { newServerRoot = it },
                        label = { Text("Root Directory") },
                        placeholder = { Text("/data/data/com.termux/files/home/www") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            nginxManager.createServerBlock(
                                serverName = newServerName,
                                port = newServerPort.toIntOrNull() ?: 8081,
                                root = newServerRoot
                            )
                            showAddServerBlockDialog = false
                            newServerName = ""
                            newServerPort = "8081"
                            newServerRoot = "/data/data/com.termux/files/home/www"
                            // Reload server blocks list
                            val result = nginxManager.listServerBlocks()
                            serverBlocks = result.output.lines().filter { it.isNotBlank() }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun PHPScreen() {
    val context = LocalContext.current
    val phpManager = remember { PHPManager(context) }
    val scope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(false) }
    var phpVersion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var phpInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        isInstalled = phpManager.isPHPInstalled()
        if (isInstalled) {
            phpVersion = phpManager.getPHPVersion()
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "PHP Manager",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF39FF14))
            }
            return@Column
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PHP Status", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isInstalled) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isInstalled) "Installed" else "Not Installed",
                                color = if (isInstalled) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }

                    if (isInstalled && phpVersion.isNotEmpty()) {
                        Text("Version: $phpVersion", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isInstalled) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                phpManager.installPHP()
                                isInstalled = phpManager.isPHPInstalled()
                                phpVersion = phpManager.getPHPVersion()
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install PHP", color = Color.Black)
                    }
                }
            }
        }

        if (isInstalled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFF39FF14)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Configuration", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Extensions", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = {
                    selectedTab = 2
                    scope.launch {
                        val result = phpManager.getPhpIniContent()
                        phpInfo = if (result.output.isEmpty()) "Unable to get PHP info" else result.output
                    }
                }) {
                    Text("PHP Info", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Configuration Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PHP Configuration (php.ini)", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Common PHP settings
                            listOf(
                                "memory_limit" to "128M",
                                "upload_max_filesize" to "20M",
                                "post_max_size" to "20M",
                                "max_execution_time" to "30",
                                "display_errors" to "On"
                            ).forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key, fontSize = 14.sp)
                                    Text(value, fontSize = 14.sp, color = Color(0xFF39FF14))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { /* TODO: Edit php.ini */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit php.ini", color = Color.Black)
                            }
                        }
                    }
                }
                1 -> {
                    // Extensions Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PHP Extensions", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Common PHP extensions
                            listOf(
                                "mbstring" to true,
                                "curl" to true,
                                "gd" to false,
                                "zip" to true,
                                "xml" to true,
                                "pdo" to true,
                                "mysqli" to false
                            ).forEach { (ext, installed) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ext)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (installed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (installed) Color(0xFF4CAF50) else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = { /* TODO: Toggle extension */ }
                                        ) {
                                            Text(if (installed) "Disable" else "Install", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // PHP Info Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item {
                                Text(
                                    phpInfo,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PostgreSQLScreen() {
    val context = LocalContext.current
    val postgresManager = remember { PostgreSQLManager(context) }
    val termuxManager = remember { TermuxManager(context) }
    val scope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var databases by remember { mutableStateOf<List<String>>(emptyList()) }
    var users by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        isInstalled = postgresManager.isPostgreSQLInstalled()
        if (isInstalled) {
            isRunning = postgresManager.isPostgreSQLRunning()
            scope.launch {
                // Simple database list using psql
                val dbResult = termuxManager.executeCommand("psql -U postgres -l -t | cut -d'|' -f1")
                databases = dbResult.output.lines().filter { it.trim().isNotBlank() && !it.contains("template") }

                // Simple user list
                val userResult = termuxManager.executeCommand("psql -U postgres -c \"SELECT usename FROM pg_user;\" -t")
                users = userResult.output.lines().filter { it.trim().isNotBlank() }
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "PostgreSQL Database",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF39FF14))
            }
            return@Column
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isRunning) "Running" else if (isInstalled) "Stopped" else "Not Installed",
                                color = if (isRunning) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isInstalled) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    postgresManager.installPostgreSQL()
                                    isInstalled = postgresManager.isPostgreSQLInstalled()
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", color = Color.Black)
                        }
                    } else {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        postgresManager.startPostgreSQL()
                                        isRunning = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Text("Start")
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        postgresManager.stopPostgreSQL()
                                        isRunning = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Text("Stop")
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    postgresManager.stopPostgreSQL()
                                    kotlinx.coroutines.delay(500)
                                    postgresManager.startPostgreSQL()
                                    isRunning = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Text("Restart")
                        }
                    }
                }
            }
        }

        if (isInstalled && isRunning) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFF39FF14)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Databases", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Users", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Query", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Databases Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Databases", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { /* TODO: Create DB */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.Black)
                                    Text("Create", color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (databases.isEmpty()) {
                                Text("No databases found", color = Color.Gray)
                            } else {
                                LazyColumn {
                                    items(databases) { db ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Storage,
                                                        contentDescription = null,
                                                        tint = Color(0xFF39FF14),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(db)
                                                }
                                                Row {
                                                    IconButton(onClick = { /* TODO: Backup */ }) {
                                                        Icon(Icons.Default.Backup, null, tint = Color(0xFF2196F3))
                                                    }
                                                    IconButton(onClick = { /* TODO: Delete */ }) {
                                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Users Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Users & Roles", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { /* TODO: Create User */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = Color.Black)
                                    Text("Create", color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (users.isEmpty()) {
                                Text("No users found", color = Color.Gray)
                            } else {
                                LazyColumn {
                                    items(users) { user ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF39FF14),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(user)
                                                }
                                                IconButton(onClick = { /* TODO: Edit Permissions */ }) {
                                                    Icon(Icons.Default.Edit, null, tint = Color(0xFF2196F3))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Query Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SQL Query Executor", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                placeholder = { Text("Enter SQL query...") },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { /* TODO: Execute Query */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Execute", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MySQLScreen() {
    val context = LocalContext.current
    val mysqlManager = remember { MySQLManager(context) }
    val termuxManager = remember { TermuxManager(context) }
    val scope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var databases by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        isInstalled = mysqlManager.isMySQLInstalled()
        if (isInstalled) {
            isRunning = mysqlManager.isMySQLRunning()
            scope.launch {
                // Simple database list using mysql command
                val dbResult = termuxManager.executeCommand("mysql -e \"SHOW DATABASES;\" | tail -n +2")
                databases = dbResult.output.lines().filter { it.trim().isNotBlank() }
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "MySQL / MariaDB",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF39FF14))
            }
            return@Column
        }

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isRunning) "Running" else if (isInstalled) "Stopped" else "Not Installed",
                                color = if (isRunning) Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isInstalled) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    mysqlManager.installMySQL()
                                    isInstalled = mysqlManager.isMySQLInstalled()
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install MySQL", color = Color.Black)
                        }
                    } else {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        mysqlManager.startMySQL()
                                        isRunning = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Text("Start")
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        mysqlManager.stopMySQL()
                                        isRunning = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Text("Stop")
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    mysqlManager.stopMySQL()
                                    kotlinx.coroutines.delay(500)
                                    mysqlManager.startMySQL()
                                    isRunning = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Text("Restart")
                        }
                    }
                }
            }
        }

        if (isInstalled && isRunning) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color(0xFF39FF14)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Databases", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Users", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Query", modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Databases Tab
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Databases", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { /* TODO: Create DB */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.Black)
                                    Text("Create", color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (databases.isEmpty()) {
                                Text("No databases found", color = Color.Gray)
                            } else {
                                LazyColumn {
                                    items(databases) { db ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Storage,
                                                        contentDescription = null,
                                                        tint = Color(0xFF39FF14),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(db)
                                                }
                                                Row {
                                                    IconButton(onClick = { /* TODO: Export */ }) {
                                                        Icon(Icons.Default.Upload, null, tint = Color(0xFF2196F3))
                                                    }
                                                    IconButton(onClick = { /* TODO: Delete */ }) {
                                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5722))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Users Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MySQL Users", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { /* TODO: Create User */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = Color.Black)
                                    Text("Create User", color = Color.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Manage MySQL users and privileges",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                2 -> {
                    // Query Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("MySQL Query Executor", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                placeholder = { Text("Enter SQL query... (e.g., SELECT * FROM users;)") },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { /* TODO: Execute Query */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14))
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Execute", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}


