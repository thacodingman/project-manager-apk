package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PorkbunManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _config = MutableStateFlow(PorkbunConfig("", ""))
    val config: StateFlow<PorkbunConfig> = _config.asStateFlow()

    private val _domains = MutableStateFlow<List<PorkbunDomain>>(emptyList())
    val domains: StateFlow<List<PorkbunDomain>> = _domains.asStateFlow()

    private val _updateLogs = MutableStateFlow<List<DNSUpdateLog>>(emptyList())
    val updateLogs: StateFlow<List<DNSUpdateLog>> = _updateLogs.asStateFlow()

    /**
     * Sauvegarde la configuration Porkbun
     */
    suspend fun saveConfig(apiKey: String, secretKey: String): CommandResult {
        val configDir = "/data/data/com.termux/files/home/.dns-config"
        termuxManager.executeCommand("mkdir -p $configDir")

        val configContent = """
            {
                "apiKey": "$apiKey",
                "secretKey": "$secretKey"
            }
        """.trimIndent()

        val result = termuxManager.executeCommand("""
            cat > $configDir/porkbun.json << 'EOF'
$configContent
EOF
        """.trimIndent())

        if (result.success) {
            loadConfig()
        }

        return result
    }

    /**
     * Charge la configuration Porkbun
     */
    suspend fun loadConfig() {
        val configFile = "/data/data/com.termux/files/home/.dns-config/porkbun.json"
        val result = termuxManager.executeCommand("cat $configFile 2>/dev/null || echo '{}'")

        if (result.success && result.output.isNotBlank()) {
            val apiKey = extractJsonField(result.output, "apiKey")
            val secretKey = extractJsonField(result.output, "secretKey")

            _config.value = PorkbunConfig(apiKey, secretKey)
        }
    }

    /**
     * Liste tous les domaines
     */
    suspend fun listDomains(): CommandResult {
        val cfg = _config.value
        if (cfg.apiKey.isBlank() || cfg.secretKey.isBlank()) {
            return CommandResult(1, "", "Configuration incomplete")
        }

        val payload = """{"apikey":"${cfg.apiKey}","secretapikey":"${cfg.secretKey}"}"""

        val result = termuxManager.executeCommand("""
            curl -s -X POST https://porkbun.com/api/json/v3/domain/listAll \
            -H "Content-Type: application/json" \
            -d '$payload'
        """.trimIndent())

        if (result.success) {
            parseDomains(result.output)
        }

        return result
    }

    /**
     * Met a jour l'enregistrement DNS A pour un domaine
     */
    suspend fun updateDNSRecord(domain: String, subdomain: String = "", ip: String = ""): CommandResult {
        val cfg = _config.value
        if (cfg.apiKey.isBlank() || cfg.secretKey.isBlank()) {
            return CommandResult(1, "", "Configuration incomplete")
        }

        val ipAddress = if (ip.isBlank()) {
            // Recuperer l'IP publique
            val ipResult = termuxManager.executeCommand("curl -s https://api.ipify.org")
            if (ipResult.success) ipResult.output.trim() else ""
        } else ip

        val subdomainPath = if (subdomain.isNotBlank()) "/$subdomain" else ""
        val payload = """
            {
                "apikey":"${cfg.apiKey}",
                "secretapikey":"${cfg.secretKey}",
                "content":"$ipAddress",
                "type":"A",
                "ttl":"300"
            }
        """.trimIndent()

        val result = termuxManager.executeCommand("""
            curl -s -X POST https://porkbun.com/api/json/v3/dns/create/$domain$subdomainPath \
            -H "Content-Type: application/json" \
            -d '$payload'
        """.trimIndent())

        if (result.success && result.output.contains("\"status\":\"SUCCESS\"")) {
            addUpdateLog("Porkbun", domain, true)
        } else {
            addUpdateLog("Porkbun", domain, false)
        }

        return result
    }

    /**
     * Recupere les enregistrements DNS d'un domaine
     */
    suspend fun getDNSRecords(domain: String): CommandResult {
        val cfg = _config.value
        if (cfg.apiKey.isBlank() || cfg.secretKey.isBlank()) {
            return CommandResult(1, "", "Configuration incomplete")
        }

        val payload = """{"apikey":"${cfg.apiKey}","secretapikey":"${cfg.secretKey}"}"""

        return termuxManager.executeCommand("""
            curl -s -X POST https://porkbun.com/api/json/v3/dns/retrieve/$domain \
            -H "Content-Type: application/json" \
            -d '$payload'
        """.trimIndent())
    }

    /**
     * Supprime un enregistrement DNS
     */
    suspend fun deleteDNSRecord(domain: String, recordId: String): CommandResult {
        val cfg = _config.value
        if (cfg.apiKey.isBlank() || cfg.secretKey.isBlank()) {
            return CommandResult(1, "", "Configuration incomplete")
        }

        val payload = """{"apikey":"${cfg.apiKey}","secretapikey":"${cfg.secretKey}"}"""

        return termuxManager.executeCommand("""
            curl -s -X POST https://porkbun.com/api/json/v3/dns/delete/$domain/$recordId \
            -H "Content-Type: application/json" \
            -d '$payload'
        """.trimIndent())
    }

    /**
     * Configure la mise a jour automatique pour un domaine
     */
    suspend fun setupAutoUpdate(domain: String, intervalMinutes: Int = 60): CommandResult {
        val cfg = _config.value
        if (cfg.apiKey.isBlank() || cfg.secretKey.isBlank()) {
            return CommandResult(1, "", "Configuration incomplete")
        }

        val cronScript = """
            #!/data/data/com.termux/files/usr/bin/bash
            IP=${'$'}(curl -s https://api.ipify.org)
            RESPONSE=${'$'}(curl -s -X POST https://porkbun.com/api/json/v3/dns/editByNameType/$domain/A \
              -H "Content-Type: application/json" \
              -d '{"apikey":"${cfg.apiKey}","secretapikey":"${cfg.secretKey}","content":"'${'$'}IP'","ttl":"300"}')
            echo "$domain: ${'$'}RESPONSE - ${'$'}(date)" >> /data/data/com.termux/files/home/.dns-logs/porkbun.log
        """.trimIndent()

        termuxManager.executeCommand("mkdir -p /data/data/com.termux/files/home/.dns-logs")
        termuxManager.executeCommand("""
            cat > /data/data/com.termux/files/home/.dns-update-porkbun.sh << 'EOF'
$cronScript
EOF
        """.trimIndent())
        termuxManager.executeCommand("chmod +x /data/data/com.termux/files/home/.dns-update-porkbun.sh")

        return termuxManager.executeCommand(
            "termux-job-scheduler --period-ms $((intervalMinutes * 60 * 1000)) --script /data/data/com.termux/files/home/.dns-update-porkbun.sh 2>&1 || echo 'Installer termux-api'"
        )
    }

    /**
     * Charge les logs
     */
    suspend fun loadLogs() {
        val logFile = "/data/data/com.termux/files/home/.dns-logs/porkbun.log"
        val result = termuxManager.executeCommand("tail -n 50 $logFile 2>/dev/null || echo ''")

        if (result.success && result.output.isNotBlank()) {
            val logs = result.output.lines()
                .filter { it.isNotBlank() }
                .take(20)
                .map { line ->
                    val parts = line.split(":")
                    val domain = parts.getOrNull(0)?.trim() ?: ""
                    val success = line.contains("\"status\":\"SUCCESS\"")

                    DNSUpdateLog(
                        service = "Porkbun",
                        domain = domain,
                        success = success,
                        message = line,
                        timestamp = System.currentTimeMillis()
                    )
                }
            _updateLogs.value = logs
        }
    }

    private fun parseDomains(json: String) {
        // Parse simple de la reponse JSON pour extraire les domaines
        val domainList = mutableListOf<PorkbunDomain>()

        val domainsPattern = """"domain"\s*:\s*"([^"]*)"""".toRegex()
        domainsPattern.findAll(json).forEach { match ->
            val domain = match.groupValues.getOrNull(1)
            if (domain != null && domain.isNotBlank()) {
                domainList.add(PorkbunDomain(domain, "active", emptyList()))
            }
        }

        _domains.value = domainList.distinctBy { it.name }
    }

    private fun addUpdateLog(service: String, domain: String, success: Boolean) {
        val log = DNSUpdateLog(
            service = service,
            domain = domain,
            success = success,
            message = if (success) "DNS mis a jour" else "echec",
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
 * Configuration Porkbun
 */
data class PorkbunConfig(
    val apiKey: String,
    val secretKey: String
)

/**
 * Domaine Porkbun
 */
data class PorkbunDomain(
    val name: String,
    val status: String,
    val records: List<DNSRecord>
)

/**
 * Enregistrement DNS
 */
data class DNSRecord(
    val id: String,
    val type: String,
    val name: String,
    val content: String,
    val ttl: Int
)
