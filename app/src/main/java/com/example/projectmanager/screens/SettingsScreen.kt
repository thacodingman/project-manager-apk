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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.projectmanager.models.*
import com.example.projectmanager.services.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val duckDNSManager = remember { DuckDNSManager(context) }
    val noIPManager = remember { NoIPManager(context) }
    val porkbunManager = remember { PorkbunManager(context) }
    val proxyManager = remember { ProxyManager(context) }
    val credentialsManager = remember { CredentialsManager(context) }
    val scope = rememberCoroutineScope()

    val duckDNSConfig by duckDNSManager.config.collectAsState()
    val duckDNSLogs by duckDNSManager.updateLogs.collectAsState()
    val noIPConfig by noIPManager.config.collectAsState()
    val noIPLogs by noIPManager.updateLogs.collectAsState()
    val porkbunConfig by porkbunManager.config.collectAsState()
    val porkbunDomains by porkbunManager.domains.collectAsState()
    val porkbunLogs by porkbunManager.updateLogs.collectAsState()
    val proxyRules by proxyManager.proxyRules.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var credentials by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Charger les configs au lancement
    LaunchedEffect(Unit) {
        duckDNSManager.loadConfig()
        duckDNSManager.loadLogs()
        noIPManager.loadConfig()
        noIPManager.loadLogs()
        porkbunManager.loadConfig()
        porkbunManager.loadLogs()
        proxyManager.loadProxyRules()
        credentials = credentialsManager.getAllCredentials()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // En-tete
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        text = "Parametres",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configuration et services DNS",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("General") },
                icon = { Icon(Icons.Default.Settings, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("DuckDNS") },
                icon = { Icon(Icons.Default.CloudUpload, null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("No-IP") },
                icon = { Icon(Icons.Default.Cloud, null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Porkbun") },
                icon = { Icon(Icons.Default.Dns, null) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Proxy") },
                icon = { Icon(Icons.Default.Security, null) }
            )
            Tab(
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                text = { Text("Credentials") },
                icon = { Icon(Icons.Default.Key, null) }
            )
        }

        // Contenu
        when (selectedTab) {
            0 -> GeneralSettingsTab(statusMessage)
            1 -> DuckDNSTab(
                config = duckDNSConfig,
                logs = duckDNSLogs,
                onSaveConfig = { token, domains ->
                    scope.launch {
                        isLoading = true
                        val result = duckDNSManager.saveConfig(token, domains)
                        statusMessage = if (result.success) {
                            "Configuration DuckDNS sauvegardee"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onUpdateIP = {
                    scope.launch {
                        isLoading = true
                        val result = duckDNSManager.updateIP()
                        statusMessage = if (result.success) {
                            "IP mise a jour: ${result.output}"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        duckDNSManager.loadLogs()
                        isLoading = false
                    }
                },
                onSetupAutoUpdate = { interval ->
                    scope.launch {
                        isLoading = true
                        val result = duckDNSManager.setupAutoUpdate(interval)
                        statusMessage = if (result.success || result.output.contains("Installer termux-api")) {
                            if (result.output.contains("Installer termux-api")) {
                                "⚠️ Installez termux-api: pkg install termux-api"
                            } else {
                                "Mise a jour automatique configuree (${interval}min)"
                            }
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onRefreshLogs = {
                    scope.launch {
                        duckDNSManager.loadLogs()
                    }
                },
                isLoading = isLoading,
                statusMessage = statusMessage
            )
            2 -> NoIPTab(
                config = noIPConfig,
                logs = noIPLogs,
                onSaveConfig = { username, password, hostnames ->
                    scope.launch {
                        isLoading = true
                        val result = noIPManager.saveConfig(username, password, hostnames.joinToString(","))
                        statusMessage = if (result.success) {
                            "Configuration No-IP sauvegardee"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onUpdateIP = {
                    scope.launch {
                        isLoading = true
                        val result = noIPManager.updateIP()
                        statusMessage = if (result.success) {
                            "IP mise a jour:\n${result.output}"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        noIPManager.loadLogs()
                        isLoading = false
                    }
                },
                onSetupAutoUpdate = { interval ->
                    scope.launch {
                        isLoading = true
                        // TODO: Implement setupAutoUpdate in NoIPManager if needed
                        statusMessage = "Mise a jour automatique No-IP non implementee"
                        isLoading = false
                    }
                },
                onRefreshLogs = {
                    scope.launch {
                        noIPManager.loadLogs()
                    }
                },
                isLoading = isLoading,
                statusMessage = statusMessage
            )
            3 -> PorkbunTab(
                config = porkbunConfig,
                domains = porkbunDomains,
                logs = porkbunLogs,
                onSaveConfig = { apiKey, secretKey ->
                    scope.launch {
                        isLoading = true
                        val result = porkbunManager.saveConfig(apiKey, secretKey)
                        statusMessage = if (result.success) {
                            "Configuration Porkbun sauvegardee"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onListDomains = {
                    scope.launch {
                        isLoading = true
                        val result = porkbunManager.listDomains()
                        statusMessage = if (result.success) {
                            "Domaines charges: ${porkbunManager.domains.value.size}"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onUpdateDNS = { domain, subdomain ->
                    scope.launch {
                        isLoading = true
                        val result = porkbunManager.updateDNSRecord(domain, subdomain)
                        statusMessage = if (result.success) {
                            "DNS mis a jour pour $domain"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        porkbunManager.loadLogs()
                        isLoading = false
                    }
                },
                onRefreshLogs = {
                    scope.launch {
                        porkbunManager.loadLogs()
                    }
                },
                isLoading = isLoading,
                statusMessage = statusMessage
            )
            4 -> ProxyTab(
                rules = proxyRules,
                onAddRule = { rule ->
                    scope.launch {
                        isLoading = true
                        val result = proxyManager.addProxyRule(rule)
                        statusMessage = if (result.success) {
                            "Regle proxy ajoutee pour ${rule.domain}"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onDeleteRule = { domain ->
                    scope.launch {
                        isLoading = true
                        val result = proxyManager.deleteProxyRule(domain)
                        statusMessage = if (result.success) {
                            "Regle proxy supprimee"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onGenerateSSL = { domain, email ->
                    scope.launch {
                        isLoading = true
                        val result = proxyManager.generateSSLCert(domain, email)
                        statusMessage = if (result.success || result.output.contains("Installer openssl")) {
                            if (result.output.contains("Installer openssl")) {
                                "⚠️ Installez openssl: pkg install openssl"
                            } else {
                                "Certificat SSL genere pour $domain"
                            }
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onReloadNginx = {
                    scope.launch {
                        isLoading = true
                        val result = proxyManager.reloadNginx()
                        statusMessage = if (result.success) {
                            "Nginx recharge"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                statusMessage = statusMessage
            )
            5 -> CredentialsTab(
                credentials = credentials,
                onSaveCredential = { key, value ->
                    scope.launch {
                        isLoading = true
                        val result = credentialsManager.saveCredential(key, value)
                        statusMessage = if (result.success) {
                            "Credential sauvegardee"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        credentials = credentialsManager.getAllCredentials()
                        isLoading = false
                    }
                },
                onResetDefaults = {
                    scope.launch {
                        isLoading = true
                        val result = credentialsManager.resetToDefaults()
                        statusMessage = if (result.success) {
                            "Credentials reinitialisees"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        credentials = credentialsManager.getAllCredentials()
                        isLoading = false
                    }
                },
                statusMessage = statusMessage,
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun GeneralSettingsTab(statusMessage: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "a propos de l'application",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version: 1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Text("Android minimum: 11 (API 30)", style = MaterialTheme.typography.bodyMedium)
                    Text("Progression: 64% (7/11 phases)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Services installes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✅ Termux", style = MaterialTheme.typography.bodySmall)
                    Text("✅ Apache HTTP Server", style = MaterialTheme.typography.bodySmall)
                    Text("✅ Nginx", style = MaterialTheme.typography.bodySmall)
                    Text("✅ PHP & PHP-FPM", style = MaterialTheme.typography.bodySmall)
                    Text("✅ PostgreSQL", style = MaterialTheme.typography.bodySmall)
                    Text("✅ MySQL/MariaDB", style = MaterialTheme.typography.bodySmall)
                    Text("✅ Strapi CMS", style = MaterialTheme.typography.bodySmall)
                    Text("✅ OpenSSH", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, null)
                        Text(
                            "Conseil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Configurez un service DNS dynamique (DuckDNS, DynDNS, No-IP) pour acceder a vos projets depuis Internet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
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
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DuckDNSTab(
    config: DuckDNSConfig,
    logs: List<DNSUpdateLog>,
    onSaveConfig: (String, List<String>) -> Unit,
    onUpdateIP: () -> Unit,
    onSetupAutoUpdate: (Int) -> Unit,
    onRefreshLogs: () -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    var token by remember { mutableStateOf(config.token) }
    var domains by remember { mutableStateOf(config.domains.joinToString("\n")) }
    var autoUpdateInterval by remember { mutableStateOf("5") }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        token = config.token
        domains = config.domains.joinToString("\n")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                        Text("a propos de DuckDNS", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "DuckDNS est un service DNS dynamique gratuit. Obtenez votre token sur duckdns.org",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token DuckDNS") },
                placeholder = { Text("Votre token depuis duckdns.org") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        item {
            OutlinedTextField(
                value = domains,
                onValueChange = { domains = it },
                label = { Text("Domaines (un par ligne)") },
                placeholder = { Text("mondomaine\nautredomaine") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !isLoading
            )
        }

        item {
            Button(
                onClick = {
                    val domainList = domains.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    onSaveConfig(token, domainList)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && token.isNotBlank() && domains.isNotBlank()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sauvegarder la configuration")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUpdateIP,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && config.token.isNotBlank()
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mettre a jour IP")
                }

                OutlinedButton(
                    onClick = { showAutoUpdateDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && config.token.isNotBlank()
                ) {
                    Icon(Icons.Default.Timer, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Update")
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
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
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historique des mises a jour",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefreshLogs) {
                    Icon(Icons.Default.Refresh, "Actualiser")
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucune mise a jour effectuee",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.success)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                log.domain,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                log.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Icon(
                            if (log.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            null,
                            tint = if (log.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showAutoUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            title = { Text("Configuration mise a jour automatique") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Intervalle de mise a jour (minutes)")
                    OutlinedTextField(
                        value = autoUpdateInterval,
                        onValueChange = { autoUpdateInterval = it },
                        label = { Text("Minutes") },
                        placeholder = { Text("5") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "⚠️ Necessite termux-api: pkg install termux-api",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val interval = autoUpdateInterval.toIntOrNull() ?: 5
                        onSetupAutoUpdate(interval)
                        showAutoUpdateDialog = false
                    }
                ) {
                    Text("Configurer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoUpdateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun NoIPTab(
    config: NoIPConfig,
    logs: List<DNSUpdateLog>,
    onSaveConfig: (String, String, List<String>) -> Unit,
    onUpdateIP: () -> Unit,
    onSetupAutoUpdate: (Int) -> Unit,
    onRefreshLogs: () -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    var username by remember { mutableStateOf(config.username) }
    var password by remember { mutableStateOf(config.password) }
    var hostname by remember { mutableStateOf(config.hostname) }
    var autoUpdateInterval by remember { mutableStateOf("30") }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        username = config.username
        password = config.password
        hostname = config.hostname
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                        Text("a propos de No-IP", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No-IP est un service DNS dynamique. Creez un compte sur noip.com",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nom d'utilisateur No-IP") },
                placeholder = { Text("votre@email.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
        }

        item {
            OutlinedTextField(
                value = hostname,
                onValueChange = { hostname = it },
                label = { Text("Hostname") },
                placeholder = { Text("monsite.ddns.net") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        item {
            Button(
                onClick = {
                    onSaveConfig(username, password, listOf(hostname))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sauvegarder la configuration")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUpdateIP,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && config.username.isNotBlank()
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mettre a jour IP")
                }

                OutlinedButton(
                    onClick = { showAutoUpdateDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && config.username.isNotBlank()
                ) {
                    Icon(Icons.Default.Timer, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Update")
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
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
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historique des mises a jour",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefreshLogs) {
                    Icon(Icons.Default.Refresh, "Actualiser")
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucune mise a jour effectuee",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.success)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                log.domain,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                log.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Icon(
                            if (log.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            null,
                            tint = if (log.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showAutoUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showAutoUpdateDialog = false },
            title = { Text("Configuration mise a jour automatique") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Intervalle de mise a jour (minutes)")
                    OutlinedTextField(
                        value = autoUpdateInterval,
                        onValueChange = { autoUpdateInterval = it },
                        label = { Text("Minutes") },
                        placeholder = { Text("30") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "⚠️ Recommande: 30 minutes minimum pour No-IP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val interval = autoUpdateInterval.toIntOrNull() ?: 30
                        onSetupAutoUpdate(interval)
                        showAutoUpdateDialog = false
                    }
                ) {
                    Text("Configurer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoUpdateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun CredentialsTab(
    credentials: Map<String, String>,
    onSaveCredential: (String, String) -> Unit,
    onResetDefaults: () -> Unit,
    statusMessage: String,
    isLoading: Boolean
) {
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, null)
                        Text(
                            "Credentials Termux",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Gerez les identifiants et variables par defaut des installations Termux",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Credentials configurees",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = onResetDefaults,
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    credentials.forEach { (key, value) ->
                        EditableCredentialItem(
                            label = formatCredentialLabel(key),
                            key = key,
                            value = value,
                            isEditing = editingKey == key,
                            editValue = editValue,
                            onEdit = {
                                editingKey = key
                                editValue = value
                            },
                            onSave = {
                                onSaveCredential(key, editValue)
                                editingKey = null
                            },
                            onCancel = { editingKey = null },
                            onValueChange = { editValue = it }
                        )
                    }
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
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
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EditableCredentialItem(
    label: String,
    key: String,
    value: String,
    isEditing: Boolean,
    editValue: String,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

        if (isEditing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = onValueChange,
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Check, "Save", modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Cancel", modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun formatCredentialLabel(key: String): String {
    return key.replace("_", " ").split(" ").joinToString(" ") {
        it.replaceFirstChar { c -> c.uppercase() }
    }
}

@Composable
private fun PorkbunTab(
    config: PorkbunConfig,
    domains: List<PorkbunDomain>,
    logs: List<DNSUpdateLog>,
    onSaveConfig: (String, String) -> Unit,
    onListDomains: () -> Unit,
    onUpdateDNS: (String, String) -> Unit,
    onRefreshLogs: () -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var secretKey by remember { mutableStateOf(config.secretKey) }

    LaunchedEffect(config) {
        apiKey = config.apiKey
        secretKey = config.secretKey
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                        Text("a propos de Porkbun", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Porkbun offre une API complete pour gerer vos domaines et DNS. Obtenez vos cles API sur porkbun.com/account/api",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text("pk1_xxxxxxxxxxxx") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        item {
            OutlinedTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                label = { Text("Secret API Key") },
                placeholder = { Text("sk1_xxxxxxxxxxxx") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
        }

        item {
            Button(
                onClick = { onSaveConfig(apiKey, secretKey) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && apiKey.isNotBlank() && secretKey.isNotBlank()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sauvegarder la configuration")
            }
        }

        item {
            OutlinedButton(
                onClick = onListDomains,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && config.apiKey.isNotBlank()
            ) {
                Icon(Icons.Default.List, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lister mes domaines")
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
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
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (domains.isNotEmpty()) {
            item {
                Text(
                    "Mes domaines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(domains) { domain ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                domain.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Statut: ${domain.status}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedButton(
                            onClick = { onUpdateDNS(domain.name, "") },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Update DNS")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historique",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefreshLogs) {
                    Icon(Icons.Default.Refresh, "Actualiser")
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucune mise a jour",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.success)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.domain, fontWeight = FontWeight.Bold)
                            Text(log.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        Icon(
                            if (log.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            null,
                            tint = if (log.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyTab(
    rules: List<ProxyRule>,
    onAddRule: (ProxyRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onGenerateSSL: (String, String) -> Unit,
    onReloadNginx: () -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSSLDialog by remember { mutableStateOf(false) }
    var selectedDomain by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, null)
                        Text("Proxy Inverse Nginx", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Configurez un proxy inverse pour rediriger vos domaines vers differents services (Apache, Strapi, etc.) avec support SSL/TLS",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter regle")
                }

                OutlinedButton(
                    onClick = onReloadNginx,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recharger Nginx")
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
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
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Text(
                "Regles configurees",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (rules.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Security,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aucune regle proxy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(rules) { rule ->
                ProxyRuleCard(
                    rule = rule,
                    onDelete = { onDeleteRule(rule.domain) },
                    onGenerateSSL = {
                        selectedDomain = rule.domain
                        showSSLDialog = true
                    },
                    isLoading = isLoading
                )
            }
        }
    }

    if (showAddDialog) {
        AddProxyRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { rule ->
                onAddRule(rule)
                showAddDialog = false
            }
        )
    }

    if (showSSLDialog) {
        GenerateSSLDialog(
            domain = selectedDomain,
            onDismiss = { showSSLDialog = false },
            onGenerate = { email ->
                onGenerateSSL(selectedDomain, email)
                showSSLDialog = false
            }
        )
    }
}

@Composable
private fun ProxyRuleCard(
    rule: ProxyRule,
    onDelete: () -> Unit,
    onGenerateSSL: () -> Unit,
    isLoading: Boolean
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rule.domain,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "→ ${rule.target}:${rule.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    if (rule.ssl) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                "SSL active",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!rule.ssl) {
                    OutlinedButton(
                        onClick = onGenerateSSL,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SSL")
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la regle ?") },
            text = { Text("La regle proxy pour ${rule.domain} sera supprimee.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun AddProxyRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (ProxyRule) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("localhost") }
    var port by remember { mutableStateOf("8080") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une regle proxy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domaine") },
                    placeholder = { Text("monsite.duckdns.org") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target (IP/hostname)") },
                    placeholder = { Text("localhost") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    placeholder = { Text("8080") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Exemple: monsite.duckdns.org → localhost:8080 (Apache)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (domain.isNotBlank() && target.isNotBlank()) {
                        onAdd(
                            ProxyRule(
                                domain = domain,
                                target = target,
                                port = port.toIntOrNull() ?: 8080,
                                ssl = false,
                                certPath = ""
                            )
                        )
                    }
                },
                enabled = domain.isNotBlank() && target.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun GenerateSSLDialog(
    domain: String,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generer certificat SSL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Domaine: $domain", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("votre@email.com") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "⚠️ Certificat auto-signe (tests). Pour production, utilisez Let's Encrypt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(email) },
                enabled = email.isNotBlank()
            ) {
                Text("Generer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
