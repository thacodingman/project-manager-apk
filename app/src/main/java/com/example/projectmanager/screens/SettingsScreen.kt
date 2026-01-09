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

    LaunchedEffect(Unit) {
        duckDNSManager.loadConfig()
        noIPManager.loadConfig()
        porkbunManager.loadConfig()
        proxyManager.loadProxyRules()
        credentials = credentialsManager.getAllCredentials()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Parametres", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Configuration et services DNS", style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("General") }, icon = { Icon(Icons.Default.Settings, null) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DuckDNS") }, icon = { Icon(Icons.Default.CloudUpload, null) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("No-IP") }, icon = { Icon(Icons.Default.Cloud, null) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Porkbun") }, icon = { Icon(Icons.Default.Dns, null) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Proxy") }, icon = { Icon(Icons.Default.Security, null) })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Credentials") }, icon = { Icon(Icons.Default.Key, null) })
        }

        when (selectedTab) {
            0 -> GeneralSettingsTab(statusMessage)
            1 -> DuckDNSTab(duckDNSConfig, duckDNSLogs, { t, d -> scope.launch { duckDNSManager.saveConfig(t, d) } }, { scope.launch { duckDNSManager.updateIP() } }, isLoading, statusMessage)
            2 -> NoIPTab(noIPConfig, noIPLogs, { u, p, h -> scope.launch { noIPManager.saveConfig(u, p, h) } }, { scope.launch { noIPManager.updateIP() } }, isLoading, statusMessage)
            3 -> PorkbunTab(porkbunConfig, porkbunDomains, porkbunLogs, { a, s -> scope.launch { porkbunManager.saveConfig(a, s) } }, { scope.launch { porkbunManager.listDomains() } }, { d, su -> scope.launch { porkbunManager.updateDNSRecord(d, su) } }, isLoading, statusMessage)
            4 -> ProxyTab(proxyRules, { r -> scope.launch { proxyManager.addProxyRule(r) } }, { d -> scope.launch { proxyManager.deleteProxyRule(d) } }, { d, e -> scope.launch { proxyManager.generateSSLCert(d, e) } }, { scope.launch { proxyManager.reloadNginx() } }, isLoading, statusMessage)
            5 -> CredentialsTab(credentials, { k, v -> scope.launch { credentialsManager.saveCredential(k, v); credentials = credentialsManager.getAllCredentials() } }, { scope.launch { credentialsManager.resetToDefaults(); credentials = credentialsManager.getAllCredentials() } }, statusMessage, isLoading)
        }
    }
}

@Composable
private fun GeneralSettingsTab(statusMessage: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Version: 1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text("Progres: 100% (Stabilisation terminee)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DuckDNSTab(config: DuckDNSConfig, logs: List<DNSUpdateLog>, onSave: (String, List<String>) -> Unit, onUpdate: () -> Unit, isLoading: Boolean, statusMessage: String) {
    var token by remember { mutableStateOf(config.token) }
    var domains by remember { mutableStateOf(config.domains.joinToString("\n")) }
    
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Token") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = domains, onValueChange = { domains = it }, label = { Text("Domaines") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(token, domains.lines().filter { it.isNotBlank() }) }, modifier = Modifier.fillMaxWidth()) { Text("Sauvegarder") }
        Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth()) { Text("Mettre a jour IP") }
        
        Text("Logs", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${log.domain}: ${log.message}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NoIPTab(config: NoIPConfig, logs: List<DNSUpdateLog>, onSave: (String, String, List<String>) -> Unit, onUpdate: () -> Unit, isLoading: Boolean, statusMessage: String) {
    var user by remember { mutableStateOf(config.username) }
    var pass by remember { mutableStateOf(config.password) }
    var host by remember { mutableStateOf(config.hostname) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Hostname") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(user, pass, listOf(host)) }, modifier = Modifier.fillMaxWidth()) { Text("Sauvegarder") }
        Button(onClick = onUpdate, modifier = Modifier.fillMaxWidth()) { Text("Mettre a jour IP") }
    }
}

@Composable
private fun PorkbunTab(config: PorkbunConfig, domains: List<PorkbunDomain>, logs: List<DNSUpdateLog>, onSave: (String, String) -> Unit, onList: () -> Unit, onUpdate: (String, String) -> Unit, isLoading: Boolean, statusMessage: String) {
    var api by remember { mutableStateOf(config.apiKey) }
    var secret by remember { mutableStateOf(config.secretKey) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = api, onValueChange = { api = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("Secret Key") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(api, secret) }, modifier = Modifier.fillMaxWidth()) { Text("Sauvegarder") }
        Button(onClick = onList, modifier = Modifier.fillMaxWidth()) { Text("Charger Domaines") }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(domains) { domain ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(domain.name)
                        TextButton(onClick = { onUpdate(domain.name, "") }) { Text("Update") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyTab(rules: List<ProxyRule>, onAdd: (ProxyRule) -> Unit, onDelete: (String) -> Unit, onSSL: (String, String) -> Unit, onReload: () -> Unit, isLoading: Boolean, statusMessage: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onReload, modifier = Modifier.fillMaxWidth()) { Text("Recharger Nginx") }
        LazyColumn {
            items(rules) { rule ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(rule.domain)
                        IconButton(onClick = { onDelete(rule.domain) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialsTab(credentials: Map<String, String>, onSave: (String, String) -> Unit, onReset: () -> Unit, statusMessage: String, isLoading: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reinitialiser") }
        credentials.forEach { (k, v) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k)
                Text(v, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
