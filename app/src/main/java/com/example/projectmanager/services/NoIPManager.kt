package com.example.projectmanager.services

import android.content.Context
import android.util.Base64
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NoIPManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _config = MutableStateFlow(NoIPConfig("", "", ""))
    val config: StateFlow<NoIPConfig> = _config.asStateFlow()

    private val _updateLogs = MutableStateFlow<List<DNSUpdateLog>>(emptyList())
    val updateLogs: StateFlow<List<DNSUpdateLog>> = _updateLogs.asStateFlow()

    /**
     * Sauvegarde la configuration No-IP
     */
    suspend fun saveConfig(username: String, password: String, hostname: String): CommandResult {
        val configDir = "/data/data/com.termux/files/home/.dns-config"
        termuxManager.executeCommand("mkdir -p $configDir")

        val configContent = """
            {
                "username": "$username",
                "password": "$password",
                "hostname": "$hostname"
            }
        """.trimIndent()

        val result = termuxManager.executeCommand("""
            cat > $configDir/noip.json << 'EOF'
$configContent
EOF
        """.trimIndent())

        if (result.success) {
            loadConfig()
        }

        return result
    }

    /**
     * Charge la configuration No-IP
     */
    suspend fun loadConfig() {
        val configFile = "/data/data/com.termux/files/home/.dns-config/noip.json"
        val result = termuxManager.executeCommand("cat $configFile 2>/dev/null || echo '{}'")

        if (result.success && result.output.isNotBlank()) {
            val username = extractJsonField(result.output, "username")
            val password = extractJsonField(result.output, "password")
            val hostname = extractJsonField(result.output, "hostname")

            _config.value = NoIPConfig(username, password, hostname)
        }
    }

    /**
     * Met a jour l'IP pour No-IP
     */
    suspend fun updateIP(): CommandResult {
        val cfg = _config.value
        if (cfg.username.isBlank() || cfg.password.isBlank() || cfg.hostname.isBlank()) {
            return CommandResult(1, "", "Configuration incomplete")
        }

        val auth = Base64.encodeToString("${cfg.username}:${cfg.password}".toByteArray(), Base64.NO_WRAP)
        val url = "https://dynupdate.no-ip.com/nic/update?hostname=${cfg.hostname}"

        val result = termuxManager.executeCommand(
            "curl -s -H 'Authorization: Basic $auth' -H 'User-Agent: ProjectManager/1.0 admin@example.com' '$url'"
        )

        if (result.success) {
            val isSuccess = result.output.contains("good") || result.output.contains("nochg")
            addUpdateLog("No-IP", cfg.hostname, isSuccess)
        }

        return result
    }

    private fun addUpdateLog(service: String, domain: String, success: Boolean) {
        val log = DNSUpdateLog(
            service = service,
            domain = domain,
            success = success,
            message = if (success) "IP mise a jour" else "echec",
            timestamp = System.currentTimeMillis()
        )
        _updateLogs.value = listOf(log) + _updateLogs.value.take(19)
    }

    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1) ?: ""
    }
}

/**
 * Configuration No-IP
 */
data class NoIPConfig(
    val username: String,
    val password: String,
    val hostname: String
)
