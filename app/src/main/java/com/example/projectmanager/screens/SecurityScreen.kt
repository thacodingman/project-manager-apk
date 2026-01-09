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

    // Verifier iptables au lancement
    LaunchedEffect(Unit) {
        val result = securityManager.checkFirewallAvailable()
        firewallInstalled = result.success && result.output.isNotBlank()

        if (firewallInstalled) {
            val rulesResult = securityManager.listFirewallRules()
            firewallRules = rulesResult.output

            val logsResult = securityManager.getSecurityLogs()
            securityLogs = logsResult.output
        }
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
                        text = "Securite",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gestion de la securite et firewall",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    Icons.Default.Security,
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
                text = { Text("Chiffrement") },
                icon = { Icon(Icons.Default.Lock, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Firewall") },
                icon = { Icon(Icons.Default.Shield, null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Mots de passe") },
                icon = { Icon(Icons.Default.Key, null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Logs") },
                icon = { Icon(Icons.Default.Description, null) }
            )
        }

        // Contenu
        when (selectedTab) {
            0 -> EncryptionTab(
                securityManager = securityManager,
                statusMessage = statusMessage,
                onStatusChange = { statusMessage = it }
            )
            1 -> FirewallTab(
                securityManager = securityManager,
                isInstalled = firewallInstalled,
                rules = firewallRules,
                onInstall = {
                    scope.launch {
                        isLoading = true
                        val result = securityManager.installFirewall()
                        statusMessage = if (result.success) {
                            firewallInstalled = true
                            "Firewall installe"
                        } else {
                            "Erreur: ${result.error}"
                        }
                        isLoading = false
                    }
                },
                onRefreshRules = {
                    scope.launch {
                        val result = securityManager.listFirewallRules()
                        firewallRules = result.output
                    }
                },
                onBlockPort = { port ->
                    scope.launch {
                        isLoading = true
                        val result = securityManager.blockPort(port)
                        statusMessage = if (result.output.contains("Permissions root")) {
                            "⚠️ Permissions root requises pour iptables"
                        } else {
                            "Port $port bloque"
                        }
                        securityManager.logSecurityEvent("Port $port bloque", "INFO")
                        isLoading = false
                    }
                },
                onBlockIP = { ip ->
                    scope.launch {
                        isLoading = true
                        val result = securityManager.blockIP(ip)
                        statusMessage = if (result.output.contains("Permissions root")) {
                            "⚠️ Permissions root requises pour iptables"
                        } else {
                            "IP $ip bloquee"
                        }
                        securityManager.logSecurityEvent("IP $ip bloquee", "WARNING")
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                statusMessage = statusMessage
            )
            2 -> PasswordTab(
                securityManager = securityManager,
                statusMessage = statusMessage,
                onStatusChange = { statusMessage = it }
            )
            3 -> SecurityLogsTab(
                logs = securityLogs,
                onRefresh = {
                    scope.launch {
                        val result = securityManager.getSecurityLogs()
                        securityLogs = result.output
                    }
                },
                onClear = {
                    scope.launch {
                        securityManager.clearSecurityLogs()
                        securityLogs = ""
                        statusMessage = "Logs effaces"
                    }
                }
            )
        }
    }
}

@Composable
private fun EncryptionTab(
    securityManager: SecurityManager,
    statusMessage: String,
    onStatusChange: (String) -> Unit
) {
    var textToEncrypt by remember { mutableStateOf("") }
    var encryptionKey by remember { mutableStateOf("") }
    var encryptedText by remember { mutableStateOf("") }
    var textToDecrypt by remember { mutableStateOf("") }
    var decryptedText by remember { mutableStateOf("") }

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
                        Icon(Icons.Default.Info, null)
                        Text("Chiffrement AES-256", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Chiffrez et dechiffrez vos donnees sensibles avec AES",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text("Chiffrer du texte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = textToEncrypt,
                onValueChange = { textToEncrypt = it },
                label = { Text("Texte a chiffrer") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = encryptionKey,
                onValueChange = { encryptionKey = it },
                label = { Text("Cle de chiffrement") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = {
                    if (textToEncrypt.isNotBlank() && encryptionKey.isNotBlank()) {
                        encryptedText = securityManager.encryptString(textToEncrypt, encryptionKey)
                        onStatusChange("Texte chiffre avec succes")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = textToEncrypt.isNotBlank() && encryptionKey.isNotBlank()
            ) {
                Icon(Icons.Default.Lock, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chiffrer")
            }
        }

        if (encryptedText.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resultat chiffre:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            encryptedText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Text("Dechiffrer du texte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = textToDecrypt,
                onValueChange = { textToDecrypt = it },
                label = { Text("Texte chiffre") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            Button(
                onClick = {
                    if (textToDecrypt.isNotBlank() && encryptionKey.isNotBlank()) {
                        decryptedText = securityManager.decryptString(textToDecrypt, encryptionKey)
                        onStatusChange(if (decryptedText.isNotBlank()) "Texte dechiffre" else "Erreur de dechiffrement")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = textToDecrypt.isNotBlank() && encryptionKey.isNotBlank()
            ) {
                Icon(Icons.Default.LockOpen, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dechiffrer")
            }
        }

        if (decryptedText.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resultat dechiffre:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(decryptedText, style = MaterialTheme.typography.bodyMedium)
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
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun FirewallTab(
    securityManager: SecurityManager,
    isInstalled: Boolean,
    rules: String,
    onInstall: () -> Unit,
    onRefreshRules: () -> Unit,
    onBlockPort: (Int) -> Unit,
    onBlockIP: (String) -> Unit,
    isLoading: Boolean,
    statusMessage: String
) {
    var portToBlock by remember { mutableStateOf("") }
    var ipToBlock by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isInstalled) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Firewall non installe", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Installez iptables pour gerer le firewall")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onInstall, enabled = !isLoading) {
                            Text("Installer iptables")
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null)
                            Text("Attention", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ Les regles iptables necessitent des permissions root sur Android. Fonctionnalites limitees sans root.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Text("Bloquer un port", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = portToBlock,
                        onValueChange = { portToBlock = it },
                        label = { Text("Port") },
                        placeholder = { Text("8080") },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            portToBlock.toIntOrNull()?.let { onBlockPort(it) }
                        },
                        enabled = !isLoading && portToBlock.toIntOrNull() != null
                    ) {
                        Icon(Icons.Default.Block, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bloquer")
                    }
                }
            }

            item {
                Text("Bloquer une IP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ipToBlock,
                        onValueChange = { ipToBlock = it },
                        label = { Text("Adresse IP") },
                        placeholder = { Text("192.168.1.100") },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { onBlockIP(ipToBlock) },
                        enabled = !isLoading && ipToBlock.isNotBlank()
                    ) {
                        Icon(Icons.Default.Block, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bloquer")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Regles actives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefreshRules) {
                        Icon(Icons.Default.Refresh, "Actualiser")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            rules.ifBlank { "Aucune regle" },
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
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
                        containerColor = if (statusMessage.contains("Erreur") || statusMessage.contains("⚠️"))
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
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PasswordTab(
    securityManager: SecurityManager,
    statusMessage: String,
    onStatusChange: (String) -> Unit
) {
    var generatedPassword by remember { mutableStateOf("") }
    var passwordToCheck by remember { mutableStateOf("") }
    var passwordStrength by remember { mutableStateOf<PasswordStrength?>(null) }

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
                        Text("Gestion des mots de passe", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generez des mots de passe securises et verifiez leur force",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text("Generer un mot de passe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Button(
                onClick = {
                    generatedPassword = securityManager.generateSecurePassword(16)
                    onStatusChange("Mot de passe genere")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Autorenew, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generer mot de passe securise")
            }
        }

        if (generatedPassword.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mot de passe genere:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            generatedPassword,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Text("Verifier la force", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            OutlinedTextField(
                value = passwordToCheck,
                onValueChange = {
                    passwordToCheck = it
                    passwordStrength = if (it.isNotBlank()) {
                        securityManager.checkPasswordStrength(it)
                    } else null
                },
                label = { Text("Mot de passe a verifier") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        passwordStrength?.let { strength ->
            item {
                val (color, text) = when (strength) {
                    PasswordStrength.WEAK -> Color(0xFFF44336) to "Faible ⚠️"
                    PasswordStrength.MEDIUM -> Color(0xFFFF9800) to "Moyen"
                    PasswordStrength.STRONG -> Color(0xFF4CAF50) to "Fort ✓"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = color.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (strength == PasswordStrength.STRONG)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Warning,
                            null,
                            tint = color
                        )
                        Text(
                            "Force: $text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityLogsTab(
    logs: String,
    onRefresh: () -> Unit,
    onClear: () -> Unit
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
            Text("Logs de securite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Actualiser")
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Delete, "Effacer", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        logs.ifBlank { "Aucun log de securite" },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
