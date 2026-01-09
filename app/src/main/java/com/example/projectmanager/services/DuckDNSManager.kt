package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DuckDNSManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _config = MutableStateFlow(DuckDNSConfig("", emptyList()))
    val config: StateFlow<DuckDNSConfig> = _config.asStateFlow()

    private val _updateLogs = MutableStateFlow<List<DNSUpdateLog>>(emptyList())
    val updateLogs: StateFlow<List<DNSUpdateLog>> = _updateLogs.asStateFlow()

    /**
     * Sauvegarde la configuration DuckDNS
     */
    suspend fun saveConfig(token: String, domains: List<String>): CommandResult {
        val configDir = "/data/data/com.termux/files/home/.dns-config"
        termuxManager.executeCommand("mkdir -p $configDir")

        val configContent = """
            {
                "token": "$token",
                "domains": [${domains.joinToString(",") { "\"$it\"" }}]
            }
        """.trimIndent()

        val result = termuxManager.executeCommand("""
            cat > $configDir/duckdns.json << 'EOF'
$configContent
EOF
        """.trimIndent())

        if (result.success) {
            loadConfig()
        }

        return result
    }

    /**
     * Charge la configuration DuckDNS
     */
    suspend fun loadConfig() {
        val configFile = "/data/data/com.termux/files/home/.dns-config/duckdns.json"
        val result = termuxManager.executeCommand("cat $configFile 2>/dev/null || echo '{}'")

        if (result.success && result.output.isNotBlank()) {
            val token = extractJsonField(result.output, "token")
            val domainsJson = extractJsonArray(result.output, "domains")

            _config.value = DuckDNSConfig(token, domainsJson)
        }
    }

    /**
     * Met a jour l'IP pour DuckDNS
     */
    suspend fun updateIP(): CommandResult {
        val cfg = _config.value
        if (cfg.token.isBlank() || cfg.domains.isEmpty()) {
            return CommandResult(false, "", "Configuration incomplete", 1)
        }

        val domainsParam = cfg.domains.joinToString(",")
        val url = "https://www.duckdns.org/update?domains=$domainsParam&token=${cfg.token}&ip="

        val result = termuxManager.executeCommand("curl -s '$url'")

        // Logger la mise a jour
        if (result.success) {
            addUpdateLog("DuckDNS", domainsParam, result.output.contains("OK"))
        }

        return result
    }

    /**
     * Configure la mise a jour automatique via cron
     */
    suspend fun setupAutoUpdate(intervalMinutes: Int = 5): CommandResult {
        val cfg = _config.value
        if (cfg.token.isBlank() || cfg.domains.isEmpty()) {
            return CommandResult(false, "", "Configuration incomplete", 1)
        }

        val domainsParam = cfg.domains.joinToString(",")
        val cronScript = """
            #!/data/data/com.termux/files/usr/bin/bash
            curl -s "https://www.duckdns.org/update?domains=$domainsParam&token=${cfg.token}&ip=" >> /data/data/com.termux/files/home/.dns-logs/duckdns.log
            echo " - \$(date)" >> /data/data/com.termux/files/home/.dns-logs/duckdns.log
        """.trimIndent()

        // Creer le script
        termuxManager.executeCommand("mkdir -p /data/data/com.termux/files/home/.dns-logs")
        termuxManager.executeCommand("""
            cat > /data/data/com.termux/files/home/.dns-update-duckdns.sh << 'EOF'
$cronScript
EOF
        """.trimIndent())
        termuxManager.executeCommand("chmod +x /data/data/com.termux/files/home/.dns-update-duckdns.sh")

        // Configurer termux-job-scheduler si disponible
        return termuxManager.executeCommand(
            "termux-job-scheduler --period-ms $((intervalMinutes * 60 * 1000)) --script /data/data/com.termux/files/home/.dns-update-duckdns.sh 2>&1 || echo 'Installer termux-api'"
        )
    }

    /**
     * Arrete la mise a jour automatique
     */
    suspend fun stopAutoUpdate(): CommandResult {
        return termuxManager.executeCommand("termux-job-scheduler --cancel-all 2>&1 || echo 'OK'")
    }

    /**
     * Charge les logs de mise a jour
     */
    suspend fun loadLogs() {
        val logFile = "/data/data/com.termux/files/home/.dns-logs/duckdns.log"
        val result = termuxManager.executeCommand("tail -n 50 $logFile 2>/dev/null || echo ''")

        if (result.success && result.output.isNotBlank()) {
            val logs = result.output.lines()
                .filter { it.isNotBlank() }
                .take(20)
                .map { line ->
                    DNSUpdateLog(
                        service = "DuckDNS",
                        domain = _config.value.domains.joinToString(","),
                        success = line.contains("OK"),
                        message = line,
                        timestamp = System.currentTimeMillis()
                    )
                }
            _updateLogs.value = logs
        }
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

    private fun extractJsonArray(json: String, field: String): List<String> {
        val pattern = """"$field"\s*:\s*\[(.*?)\]""".toRegex()
        val match = pattern.find(json)?.groupValues?.getOrNull(1) ?: return emptyList()
        return match.split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}

/**
 * Configuration DuckDNS
 */
data class DuckDNSConfig(
    val token: String,
    val domains: List<String>
)

/**
 * Log de mise a jour DNS
 */
data class DNSUpdateLog(
    val service: String,
    val domain: String,
    val success: Boolean,
    val message: String,
    val timestamp: Long
)
