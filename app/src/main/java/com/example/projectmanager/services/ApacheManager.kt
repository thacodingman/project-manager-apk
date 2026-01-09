package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApacheManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _serviceInfo = MutableStateFlow(
        ServiceInfo(
            name = "Apache",
            status = ServiceStatus.UNKNOWN,
            isInstalled = false,
            port = 8080,
            configPath = "/data/data/com.termux/files/usr/etc/apache2/httpd.conf",
            logPath = "/data/data/com.termux/files/usr/var/log/apache2/"
        )
    )
    val serviceInfo: StateFlow<ServiceInfo> = _serviceInfo.asStateFlow()

    private val _virtualHosts = MutableStateFlow<List<VirtualHost>>(emptyList())
    val virtualHosts: StateFlow<List<VirtualHost>> = _virtualHosts.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /**
     * Verifie si Apache est installe
     */
    suspend fun checkInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which httpd")
        _serviceInfo.value = _serviceInfo.value.copy(
            isInstalled = result.success && result.output.isNotBlank()
        )
        return result
    }

    /**
     * Installe Apache
     */
    suspend fun install(): CommandResult {
        _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.INSTALLING)
        val result = termuxManager.installApache()
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
     * Configuration initiale d'Apache
     */
    private suspend fun configureInitial() {
        // Creer les dossiers necessaires
        termuxManager.executeCommand("mkdir -p ${'$'}PREFIX/var/www/html")
        termuxManager.executeCommand("mkdir -p ${'$'}PREFIX/etc/apache2/sites-available")
        termuxManager.executeCommand("mkdir -p ${'$'}PREFIX/etc/apache2/sites-enabled")

        // Creer une page d'accueil par defaut
        termuxManager.executeCommand("""
            echo '<html><body><h1>Apache fonctionne sur Android!</h1></body></html>' > ${'$'}PREFIX/var/www/html/index.html
        """.trimIndent())
    }

    /**
     * Demarre Apache
     */
    suspend fun start(): CommandResult {
        val result = termuxManager.startApache()
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Arrete Apache
     */
    suspend fun stop(): CommandResult {
        val result = termuxManager.stopApache()
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.STOPPED)
        }
        return result
    }

    /**
     * Redemarre Apache
     */
    suspend fun restart(): CommandResult {
        val result = termuxManager.executeCommand("apachectl restart")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Verifie le statut d'Apache
     */
    suspend fun checkStatus(): CommandResult {
        val result = termuxManager.executeCommand("pgrep -f httpd")
        val isRunning = result.success && result.output.isNotBlank()
        _serviceInfo.value = _serviceInfo.value.copy(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED
        )
        return result
    }

    /**
     * Recupere la version d'Apache
     */
    suspend fun getVersion(): CommandResult {
        val result = termuxManager.executeCommand("httpd -v")
        if (result.success) {
            // Extraire la version depuis la sortie
            val versionRegex = """Apache/(\d+\.\d+\.\d+)""".toRegex()
            val version = versionRegex.find(result.output)?.groupValues?.get(1) ?: ""
            _serviceInfo.value = _serviceInfo.value.copy(version = version)
        }
        return result
    }

    /**
     * Cree un nouveau Virtual Host
     */
    suspend fun createVirtualHost(
        serverName: String,
        documentRoot: String,
        port: Int = 8080
    ): CommandResult {
        val vhostConfig = """
            <VirtualHost *:$port>
                ServerName $serverName
                DocumentRoot $documentRoot
                
                <Directory $documentRoot>
                    Options Indexes FollowSymLinks
                    AllowOverride All
                    Require all granted
                </Directory>
                
                ErrorLog ${'$'}PREFIX/var/log/apache2/${serverName}-error.log
                CustomLog ${'$'}PREFIX/var/log/apache2/${serverName}-access.log combined
            </VirtualHost>
        """.trimIndent()

        val configFile = "${'$'}PREFIX/etc/apache2/sites-available/${serverName}.conf"

        // Creer le fichier de configuration
        val createResult = termuxManager.executeCommand("""
            cat > $configFile << 'EOF'
            $vhostConfig
            EOF
        """.trimIndent())

        if (createResult.success) {
            // Creer le document root
            termuxManager.executeCommand("mkdir -p $documentRoot")

            // Activer le site
            termuxManager.executeCommand("""
                ln -sf $configFile ${'$'}PREFIX/etc/apache2/sites-enabled/${serverName}.conf
            """.trimIndent())

            // Ajouter a la liste
            val newVHost = VirtualHost(
                id = serverName,
                serverName = serverName,
                documentRoot = documentRoot,
                port = port,
                enabled = true,
                configFile = configFile
            )
            _virtualHosts.value = _virtualHosts.value + newVHost
        }

        return createResult
    }

    /**
     * Liste les Virtual Hosts
     */
    suspend fun listVirtualHosts(): CommandResult {
        val result = termuxManager.executeCommand("""
            ls -1 ${'$'}PREFIX/etc/apache2/sites-available/*.conf 2>/dev/null || echo "Aucun"
        """.trimIndent())

        // Parser les resultats et mettre a jour la liste
        // (Implementation simplifiee)

        return result
    }

    /**
     * Active un Virtual Host
     */
    suspend fun enableVirtualHost(serverName: String): CommandResult {
        return termuxManager.executeCommand("""
            ln -sf ${'$'}PREFIX/etc/apache2/sites-available/${serverName}.conf ${'$'}PREFIX/etc/apache2/sites-enabled/${serverName}.conf
        """.trimIndent())
    }

    /**
     * Desactive un Virtual Host
     */
    suspend fun disableVirtualHost(serverName: String): CommandResult {
        return termuxManager.executeCommand("""
            rm -f ${'$'}PREFIX/etc/apache2/sites-enabled/${serverName}.conf
        """.trimIndent())
    }

    /**
     * Recupere les logs d'erreur
     */
    suspend fun getErrorLogs(lines: Int = 50): CommandResult {
        val result = termuxManager.executeCommand("""
            tail -n $lines ${'$'}PREFIX/var/log/apache2/error_log 2>/dev/null || echo "Pas de logs"
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
            tail -n $lines ${'$'}PREFIX/var/log/apache2/access_log 2>/dev/null || echo "Pas de logs"
        """.trimIndent())
    }

    /**
     * Teste la configuration
     */
    suspend fun testConfig(): CommandResult {
        return termuxManager.executeCommand("apachectl configtest")
    }

    /**
     * Change le port d'ecoute
     */
    suspend fun changePort(newPort: Int): CommandResult {
        val result = termuxManager.executeCommand("""
            sed -i 's/Listen [0-9]*/Listen $newPort/' ${'$'}PREFIX/etc/apache2/httpd.conf
        """.trimIndent())

        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(port = newPort)
        }

        return result
    }
}

