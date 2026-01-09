package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProxyManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _proxyRules = MutableStateFlow<List<ProxyRule>>(emptyList())
    val proxyRules: StateFlow<List<ProxyRule>> = _proxyRules.asStateFlow()

    /**
     * Charge toutes les regles de proxy
     */
    suspend fun loadProxyRules() {
        val configDir = "/data/data/com.termux/files/home/.proxy-rules"

        val result = termuxManager.executeCommand("find $configDir -name '*.json' -type f 2>/dev/null || echo ''")

        if (result.success && result.output.isNotBlank()) {
            val rulesList = mutableListOf<ProxyRule>()
            result.output.lines().forEach { filePath ->
                if (filePath.trim().isNotBlank()) {
                    val contentResult = termuxManager.executeCommand("cat $filePath")
                    if (contentResult.success) {
                        val domain = extractJsonField(contentResult.output, "domain")
                        val target = extractJsonField(contentResult.output, "target")
                        val port = extractJsonField(contentResult.output, "port").toIntOrNull() ?: 80
                        val ssl = extractJsonField(contentResult.output, "ssl") == "true"
                        val certPath = extractJsonField(contentResult.output, "certPath")

                        rulesList.add(
                            ProxyRule(
                                domain = domain,
                                target = target,
                                port = port,
                                ssl = ssl,
                                certPath = certPath
                            )
                        )
                    }
                }
            }
            _proxyRules.value = rulesList
        }
    }

    /**
     * Ajoute une regle de proxy
     */
    suspend fun addProxyRule(rule: ProxyRule): CommandResult {
        val configDir = "/data/data/com.termux/files/home/.proxy-rules"
        termuxManager.executeCommand("mkdir -p $configDir")

        val fileName = "${rule.domain.replace(".", "_")}.json"
        val jsonContent = """
            {
                "domain": "${rule.domain}",
                "target": "${rule.target}",
                "port": ${rule.port},
                "ssl": ${rule.ssl},
                "certPath": "${rule.certPath}"
            }
        """.trimIndent()

        val result = termuxManager.executeCommand("""
            cat > $configDir/$fileName << 'EOF'
$jsonContent
EOF
        """.trimIndent())

        if (result.success) {
            // Creer la configuration Nginx pour ce proxy
            val nginxResult = createNginxProxyConfig(rule)
            if (nginxResult.success) {
                loadProxyRules()
            }
            return nginxResult
        }

        return result
    }

    /**
     * Supprime une regle de proxy
     */
    suspend fun deleteProxyRule(domain: String): CommandResult {
        val configDir = "/data/data/com.termux/files/home/.proxy-rules"
        val fileName = "${domain.replace(".", "_")}.json"

        // Supprimer la config Nginx
        val nginxConfig = "/data/data/com.termux/files/usr/etc/nginx/sites-available/proxy_$domain"
        termuxManager.executeCommand("rm -f $nginxConfig")
        termuxManager.executeCommand("rm -f /data/data/com.termux/files/usr/etc/nginx/sites-enabled/proxy_$domain")

        val result = termuxManager.executeCommand("rm -f $configDir/$fileName")

        if (result.success) {
            loadProxyRules()
            // Recharger Nginx
            termuxManager.executeCommand("nginx -s reload 2>/dev/null || true")
        }

        return result
    }

    /**
     * Cree la configuration Nginx pour le proxy inverse
     */
    private suspend fun createNginxProxyConfig(rule: ProxyRule): CommandResult {
        val sitesAvailable = "/data/data/com.termux/files/usr/etc/nginx/sites-available"
        val sitesEnabled = "/data/data/com.termux/files/usr/etc/nginx/sites-enabled"

        // Creer les repertoires s'ils n'existent pas
        termuxManager.executeCommand("mkdir -p $sitesAvailable")
        termuxManager.executeCommand("mkdir -p $sitesEnabled")

        val sslConfig = if (rule.ssl && rule.certPath.isNotBlank()) {
            """
    listen 443 ssl;
    ssl_certificate ${rule.certPath}/fullchain.pem;
    ssl_certificate_key ${rule.certPath}/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
            """.trimIndent()
        } else {
            ""
        }

        val nginxConfig = """
server {
    listen 80;
    server_name ${rule.domain};
    $sslConfig

    location / {
        proxy_pass http://${rule.target}:${rule.port};
        proxy_set_header Host ${'$'}host;
        proxy_set_header X-Real-IP ${'$'}remote_addr;
        proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto ${'$'}scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade ${'$'}http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
        """.trimIndent()

        val configFile = "$sitesAvailable/proxy_${rule.domain}"
        val result = termuxManager.executeCommand("""
            cat > $configFile << 'EOF'
$nginxConfig
EOF
        """.trimIndent())

        if (result.success) {
            // Activer le site
            termuxManager.executeCommand("ln -sf $configFile $sitesEnabled/proxy_${rule.domain}")
            // Recharger Nginx
            termuxManager.executeCommand("nginx -s reload 2>/dev/null || true")
        }

        return result
    }

    /**
     * Genere un certificat SSL avec Let's Encrypt (simulation)
     */
    suspend fun generateSSLCert(domain: String, email: String): CommandResult {
        // Note: Ceci est une simulation. Pour Let's Encrypt reel, il faudrait certbot
        val certDir = "/data/data/com.termux/files/home/.ssl-certs/$domain"

        termuxManager.executeCommand("mkdir -p $certDir")

        // Creer un certificat auto-signe pour les tests
        return termuxManager.executeCommand("""
            openssl req -x509 -newkey rsa:4096 -keyout $certDir/privkey.pem \
            -out $certDir/fullchain.pem -days 365 -nodes \
            -subj "/CN=$domain/emailAddress=$email" 2>&1 || echo 'Installer openssl: pkg install openssl'
        """.trimIndent())
    }

    /**
     * Liste tous les certificats SSL
     */
    suspend fun listSSLCerts(): CommandResult {
        return termuxManager.executeCommand("""
            find /data/data/com.termux/files/home/.ssl-certs -name 'fullchain.pem' 2>/dev/null || echo ''
        """.trimIndent())
    }

    /**
     * Teste la configuration Nginx
     */
    suspend fun testNginxConfig(): CommandResult {
        return termuxManager.executeCommand("nginx -t 2>&1")
    }

    /**
     * Recharge Nginx
     */
    suspend fun reloadNginx(): CommandResult {
        return termuxManager.executeCommand("nginx -s reload 2>&1")
    }

    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1) ?: ""
    }
}

/**
 * Regle de proxy inverse
 */
data class ProxyRule(
    val domain: String,
    val target: String,
    val port: Int,
    val ssl: Boolean = false,
    val certPath: String = ""
)

