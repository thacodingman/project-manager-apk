package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NginxManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _serviceInfo = MutableStateFlow(
        ServiceInfo(
            name = "Nginx",
            status = ServiceStatus.UNKNOWN,
            isInstalled = false,
            port = 8080,
            configPath = "/data/data/com.termux/files/usr/etc/nginx/nginx.conf",
            logPath = "/data/data/com.termux/files/usr/var/log/nginx/"
        )
    )
    val serviceInfo: StateFlow<ServiceInfo> = _serviceInfo.asStateFlow()

    private val _serverBlocks = MutableStateFlow<List<ServerBlock>>(emptyList())
    val serverBlocks: StateFlow<List<ServerBlock>> = _serverBlocks.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /**
     * Verifie si Nginx est installe
     */
    suspend fun checkInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which nginx")
        _serviceInfo.value = _serviceInfo.value.copy(
            isInstalled = result.success && result.output.isNotBlank()
        )
        return result
    }

    /**
     * Installe Nginx
     */
    suspend fun install(): CommandResult {
        _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.INSTALLING)
        val result = termuxManager.installNginx()
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(
                isInstalled = true,
                status = ServiceStatus.STOPPED
            )
            // Configuration initiale
            configureInitial()
        } else {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
        }
        return result
    }

    /**
     * Configuration initiale de Nginx
     */
    private suspend fun configureInitial() {
        // Creer les dossiers necessaires
        termuxManager.executeCommand("mkdir -p ${'$'}PREFIX/share/nginx/html")
        termuxManager.executeCommand("mkdir -p ${'$'}PREFIX/etc/nginx/sites-available")
        termuxManager.executeCommand("mkdir -p ${'$'}PREFIX/etc/nginx/sites-enabled")

        // Creer une page d'accueil par defaut
        termuxManager.executeCommand("""
            echo '<html><body><h1>Nginx fonctionne sur Android!</h1></body></html>' > ${'$'}PREFIX/share/nginx/html/index.html
        """.trimIndent())

        // Ajouter l'inclusion des sites dans nginx.conf si necessaire
        termuxManager.executeCommand("""
            grep -q "sites-enabled" ${'$'}PREFIX/etc/nginx/nginx.conf || \
            sed -i '/http {/a\    include /data/data/com.termux/files/usr/etc/nginx/sites-enabled/*;' ${'$'}PREFIX/etc/nginx/nginx.conf
        """.trimIndent())
    }

    /**
     * Demarre Nginx
     */
    suspend fun start(): CommandResult {
        val result = termuxManager.startNginx()
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Arrete Nginx
     */
    suspend fun stop(): CommandResult {
        val result = termuxManager.stopNginx()
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.STOPPED)
        }
        return result
    }

    /**
     * Redemarre Nginx
     */
    suspend fun restart(): CommandResult {
        val result = termuxManager.executeCommand("nginx -s reload")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Recharge la configuration
     */
    suspend fun reload(): CommandResult {
        return termuxManager.executeCommand("nginx -s reload")
    }

    /**
     * Verifie le statut de Nginx
     */
    suspend fun checkStatus(): CommandResult {
        val result = termuxManager.executeCommand("pgrep -f nginx")
        val isRunning = result.success && result.output.isNotBlank()
        _serviceInfo.value = _serviceInfo.value.copy(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED
        )
        return result
    }

    /**
     * Recupere la version de Nginx
     */
    suspend fun getVersion(): CommandResult {
        val result = termuxManager.executeCommand("nginx -v 2>&1")
        if (result.success || result.error.isNotBlank()) {
            // nginx -v ecrit sur stderr
            val output = result.error.ifBlank { result.output }
            val versionRegex = """nginx/(\d+\.\d+\.\d+)""".toRegex()
            val version = versionRegex.find(output)?.groupValues?.get(1) ?: ""
            _serviceInfo.value = _serviceInfo.value.copy(version = version)
        }
        return result
    }

    /**
     * Cree un nouveau Server Block
     */
    suspend fun createServerBlock(
        serverName: String,
        root: String,
        port: Int = 8080,
        isProxy: Boolean = false,
        proxyPass: String = ""
    ): CommandResult {
        val serverConfig = if (isProxy) {
            """
            server {
                listen $port;
                server_name $serverName;
                
                location / {
                    proxy_pass $proxyPass;
                    proxy_set_header Host ${'$'}host;
                    proxy_set_header X-Real-IP ${'$'}remote_addr;
                    proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
                    proxy_set_header X-Forwarded-Proto ${'$'}scheme;
                }
                
                error_log ${'$'}PREFIX/var/log/nginx/${serverName}-error.log;
                access_log ${'$'}PREFIX/var/log/nginx/${serverName}-access.log;
            }
            """.trimIndent()
        } else {
            """
            server {
                listen $port;
                server_name $serverName;
                root $root;
                index index.html index.htm index.php;
                
                location / {
                    try_files ${'$'}uri ${'$'}uri/ =404;
                }
                
                error_log ${'$'}PREFIX/var/log/nginx/${serverName}-error.log;
                access_log ${'$'}PREFIX/var/log/nginx/${serverName}-access.log;
            }
            """.trimIndent()
        }

        val configFile = "${'$'}PREFIX/etc/nginx/sites-available/${serverName}.conf"

        // Creer le fichier de configuration
        val createResult = termuxManager.executeCommand("""
            cat > $configFile << 'EOF'
            $serverConfig
            EOF
        """.trimIndent())

        if (createResult.success) {
            if (!isProxy) {
                // Creer le document root
                termuxManager.executeCommand("mkdir -p $root")
            }

            // Activer le site
            termuxManager.executeCommand("""
                ln -sf $configFile ${'$'}PREFIX/etc/nginx/sites-enabled/${serverName}.conf
            """.trimIndent())

            // Ajouter a la liste
            val newServerBlock = ServerBlock(
                id = serverName,
                serverName = serverName,
                root = root,
                port = port,
                enabled = true,
                configFile = configFile,
                isProxy = isProxy,
                proxyPass = proxyPass
            )
            _serverBlocks.value = _serverBlocks.value + newServerBlock
        }

        return createResult
    }

    /**
     * Liste les Server Blocks
     */
    suspend fun listServerBlocks(): CommandResult {
        val result = termuxManager.executeCommand("""
            ls -1 ${'$'}PREFIX/etc/nginx/sites-available/*.conf 2>/dev/null || echo "Aucun"
        """.trimIndent())

        return result
    }

    /**
     * Active un Server Block
     */
    suspend fun enableServerBlock(serverName: String): CommandResult {
        return termuxManager.executeCommand("""
            ln -sf ${'$'}PREFIX/etc/nginx/sites-available/${serverName}.conf ${'$'}PREFIX/etc/nginx/sites-enabled/${serverName}.conf
        """.trimIndent())
    }

    /**
     * Desactive un Server Block
     */
    suspend fun disableServerBlock(serverName: String): CommandResult {
        return termuxManager.executeCommand("""
            rm -f ${'$'}PREFIX/etc/nginx/sites-enabled/${serverName}.conf
        """.trimIndent())
    }

    /**
     * Recupere les logs d'erreur
     */
    suspend fun getErrorLogs(lines: Int = 50): CommandResult {
        val result = termuxManager.executeCommand("""
            tail -n $lines ${'$'}PREFIX/var/log/nginx/error.log 2>/dev/null || echo "Pas de logs"
        """.trimIndent())

        if (result.success) {
            _logs.value = result.output.lines()
        }

        return result
    }

    /**
     * Recupere les logs d'acces
     */
    suspend fun getAccessLogs(lines: Int = 50): CommandResult {
        return termuxManager.executeCommand("""
            tail -n $lines ${'$'}PREFIX/var/log/nginx/access.log 2>/dev/null || echo "Pas de logs"
        """.trimIndent())
    }

    /**
     * Teste la configuration
     */
    suspend fun testConfig(): CommandResult {
        return termuxManager.executeCommand("nginx -t 2>&1")
    }

    /**
     * Change le port d'ecoute par defaut
     */
    suspend fun changePort(newPort: Int): CommandResult {
        val result = termuxManager.executeCommand("""
            sed -i 's/listen [0-9]*/listen $newPort/' ${'$'}PREFIX/etc/nginx/nginx.conf
        """.trimIndent())

        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(port = newPort)
        }

        return result
    }
}
