package com.example.projectmanager.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import com.example.projectmanager.services.SecurityManager
import com.example.projectmanager.services.PasswordStrength
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen() {
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var firewallInstalled by remember { mutableStateOf(false) }
    var firewallRules by remember { mutableStateOf("") }
    var securityLogs by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val result = securityManager.checkFirewallAvailable()
        firewallInstalled = result.success && result.output.isNotBlank()
        if (firewallInstalled) {
            firewallRules = securityManager.listFirewallRules().output
            securityLogs = securityManager.getSecurityLogs().output
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Securite", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Gestion de la securite et firewall", style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Default.Security, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chiffrement") }, icon = { Icon(Icons.Default.Lock, null) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Firewall") }, icon = { Icon(Icons.Default.Shield, null) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Mots de passe") }, icon = { Icon(Icons.Default.Key, null) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Logs") }, icon = { Icon(Icons.Default.Description, null) })
        }

        when (selectedTab) {
            0 -> EncryptionTab(securityManager, statusMessage) { statusMessage = it }
            1 -> FirewallTab(securityManager, firewallInstalled, firewallRules, { scope.launch { securityManager.installFirewall(); firewallInstalled = true } }, { scope.launch { firewallRules = securityManager.listFirewallRules().output } }, { p -> scope.launch { securityManager.blockPort(p) } }, { i -> scope.launch { securityManager.blockIP(i) } }, isLoading, statusMessage)
            2 -> PasswordTab(securityManager, statusMessage) { statusMessage = it }
            3 -> SecurityLogsTab(securityLogs, { scope.launch { securityLogs = securityManager.getSecurityLogs().output } }, { scope.launch { securityManager.clearSecurityLogs(); securityLogs = "" } })
        }
    }
}

@Composable
private fun EncryptionTab(securityManager: SecurityManager, statusMessage: String, onStatusChange: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Texte") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Cle") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { result = securityManager.encryptString(text, key); onStatusChange("Chiffre") }, modifier = Modifier.fillMaxWidth()) { Text("Chiffrer") }
            if (result.isNotBlank()) Text("Resultat: $result", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun FirewallTab(securityManager: SecurityManager, isInstalled: Boolean, rules: String, onInstall: () -> Unit, onRefresh: () -> Unit, onBlockPort: (Int) -> Unit, onBlockIP: (String) -> Unit, isLoading: Boolean, statusMessage: String) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isInstalled) {
            item { Button(onClick = onInstall) { Text("Installer iptables") } }
        } else {
            item {
                Text("Regles actives", fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) { Text(rules.ifBlank { "Aucune regle" }, modifier = Modifier.padding(8.dp), fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Actualiser") }
            }
        }
    }
}

@Composable
private fun PasswordTab(securityManager: SecurityManager, statusMessage: String, onStatusChange: (String) -> Unit) {
    var pass by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { pass = securityManager.generateSecurePassword(16); onStatusChange("Genere") }, modifier = Modifier.fillMaxWidth()) { Text("Generer mot de passe") }
        if (pass.isNotBlank()) SelectionContainer { Text(pass, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
    }
}

@Composable
private fun SecurityLogsTab(logs: String, onRefresh: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Logs", fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
                IconButton(onClick = onClear) { Icon(Icons.Default.Delete, null) }
            }
        }
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(modifier = Modifier.padding(8.dp)) { item { Text(logs.ifBlank { "Vide" }, fontSize = 10.sp, fontFamily = FontFamily.Monospace) } }
        }
    }
}

@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    Box { content() }
}
