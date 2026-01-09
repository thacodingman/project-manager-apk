package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PHPManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _serviceInfo = MutableStateFlow(
        ServiceInfo(
            name = "PHP",
            status = ServiceStatus.UNKNOWN,
            isInstalled = false,
            port = 9000, // Port par defaut PHP-FPM
            configPath = "/data/data/com.termux/files/usr/etc/php-fpm.conf",
            logPath = "/data/data/com.termux/files/usr/var/log/"
        )
    )
    val serviceInfo: StateFlow<ServiceInfo> = _serviceInfo.asStateFlow()

    private val _phpExtensions = MutableStateFlow<List<PHPExtension>>(emptyList())
    val phpExtensions: StateFlow<List<PHPExtension>> = _phpExtensions.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /**
     * Verifie si PHP est installe
     */
    suspend fun checkInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which php")
        _serviceInfo.value = _serviceInfo.value.copy(
            isInstalled = result.success && result.output.isNotBlank()
        )
        return result
    }

    /**
     * Installe PHP et PHP-FPM
     */
    suspend fun install(): CommandResult {
        _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.INSTALLING)

        // Installer PHP et PHP-FPM
        val result = termuxManager.executeCommand("pkg install -y php php-fpm")

        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(
                isInstalled = true,
                status = ServiceStatus.STOPPED
            )
            // Configuration initiale
            configureInitial()
            // Charger les extensions disponibles
            loadAvailableExtensions()
        } else {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
        }
        return result
    }

    /**
     * Configuration initiale de PHP
     */
    private suspend fun configureInitial() {
        // Copier le fichier php.ini par defaut si necessaire
        termuxManager.executeCommand("""
            if [ ! -f ${'$'}PREFIX/etc/php.ini ]; then
                cp ${'$'}PREFIX/etc/php.ini-development ${'$'}PREFIX/etc/php.ini
            fi
        """.trimIndent())

        // Configuration PHP-FPM
        termuxManager.executeCommand("""
            if [ ! -f ${'$'}PREFIX/etc/php-fpm.conf ]; then
                echo '[global]
error_log = ${'$'}PREFIX/var/log/php-fpm.log

[www]
user = nobody
group = nobody
listen = 127.0.0.1:9000
listen.owner = nobody
listen.group = nobody
pm = dynamic
pm.max_children = 5
pm.start_servers = 2
pm.min_spare_servers = 1
pm.max_spare_servers = 3' > ${'$'}PREFIX/etc/php-fpm.conf
            fi
        """.trimIndent())

        // Creer un fichier phpinfo pour test
        termuxManager.executeCommand("""
            mkdir -p ${'$'}PREFIX/var/www/html
            echo '<?php phpinfo(); ?>' > ${'$'}PREFIX/var/www/html/phpinfo.php
        """.trimIndent())
    }

    /**
     * Demarre PHP-FPM
     */
    suspend fun start(): CommandResult {
        val result = termuxManager.executeCommand("php-fpm")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Arrete PHP-FPM
     */
    suspend fun stop(): CommandResult {
        val result = termuxManager.executeCommand("pkill -f php-fpm")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.STOPPED)
        }
        return result
    }

    /**
     * Redemarre PHP-FPM
     */
    suspend fun restart(): CommandResult {
        stop()
        return start()
    }

    /**
     * Verifie le statut de PHP-FPM
     */
    suspend fun checkStatus(): CommandResult {
        val result = termuxManager.executeCommand("pgrep -f php-fpm")
        val isRunning = result.success && result.output.isNotBlank()
        _serviceInfo.value = _serviceInfo.value.copy(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED
        )
        return result
    }

    /**
     * Recupere la version de PHP
     */
    suspend fun getVersion(): CommandResult {
        val result = termuxManager.executeCommand("php -v | head -n 1")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(version = result.output.trim())
        }
        return result
    }

    /**
     * Charge les extensions PHP disponibles
     */
    private suspend fun loadAvailableExtensions() {
        // Extensions communes disponibles dans Termux
        val commonExtensions = listOf(
            "php-apache", "php-fpm", "php-pgsql", "php-redis",
            "php-curl", "php-gd", "php-mbstring", "php-xml",
            "php-zip", "php-sqlite", "php-intl"
        )

        val extensions = mutableListOf<PHPExtension>()

        for (ext in commonExtensions) {
            val checkResult = termuxManager.executeCommand("pkg list-installed | grep -i $ext")
            val isInstalled = checkResult.success && checkResult.output.isNotBlank()

            extensions.add(
                PHPExtension(
                    name = ext,
                    displayName = ext.replace("php-", "").uppercase(),
                    isInstalled = isInstalled,
                    description = getExtensionDescription(ext)
                )
            )
        }

        _phpExtensions.value = extensions
    }

    /**
     * Installe une extension PHP
     */
    suspend fun installExtension(extensionName: String): CommandResult {
        val result = termuxManager.executeCommand("pkg install -y $extensionName")
        if (result.success) {
            loadAvailableExtensions()
        }
        return result
    }

    /**
     * Desinstalle une extension PHP
     */
    suspend fun uninstallExtension(extensionName: String): CommandResult {
        val result = termuxManager.executeCommand("pkg uninstall -y $extensionName")
        if (result.success) {
            loadAvailableExtensions()
        }
        return result
    }

    /**
     * Recupere la configuration php.ini
     */
    suspend fun getPhpIniContent(): CommandResult {
        return termuxManager.executeCommand("cat ${'$'}PREFIX/etc/php.ini")
    }

    /**
     * Modifie une directive dans php.ini
     */
    suspend fun updatePhpIniDirective(directive: String, value: String): CommandResult {
        // Sauvegarde du fichier original
        termuxManager.executeCommand("cp ${'$'}PREFIX/etc/php.ini ${'$'}PREFIX/etc/php.ini.bak")

        // Modification de la directive
        return termuxManager.executeCommand("""
            sed -i 's/^;*\s*$directive\s*=.*/$directive = $value/' ${'$'}PREFIX/etc/php.ini
        """.trimIndent())
    }

    /**
     * Recupere les directives importantes de php.ini
     */
    suspend fun getImportantDirectives(): Map<String, String> {
        val directives = mutableMapOf<String, String>()

        val importantKeys = listOf(
            "memory_limit",
            "upload_max_filesize",
            "post_max_size",
            "max_execution_time",
            "max_input_time",
            "display_errors",
            "error_reporting"
        )

        for (key in importantKeys) {
            val result = termuxManager.executeCommand(
                "grep -E '^$key' ${'$'}PREFIX/etc/php.ini | cut -d'=' -f2 | tr -d ' '"
            )
            if (result.success && result.output.isNotBlank()) {
                directives[key] = result.output.trim()
            }
        }

        return directives
    }

    /**
     * Recupere les modules PHP charges
     */
    suspend fun getLoadedModules(): CommandResult {
        return termuxManager.executeCommand("php -m")
    }

    /**
     * Recupere les logs PHP-FPM
     */
    suspend fun getLogs(): CommandResult {
        val result = termuxManager.executeCommand("tail -n 100 ${'$'}PREFIX/var/log/php-fpm.log 2>/dev/null || echo 'Aucun log disponible'")
        if (result.success) {
            _logs.value = result.output.lines()
        }
        return result
    }

    /**
     * Change le port PHP-FPM
     */
    suspend fun changePort(newPort: Int): CommandResult {
        val result = termuxManager.executeCommand("""
            sed -i 's/listen = 127.0.0.1:[0-9]*/listen = 127.0.0.1:$newPort/' ${'$'}PREFIX/etc/php-fpm.conf
        """.trimIndent())

        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(port = newPort)
        }

        return result
    }

    /**
     * Teste la configuration PHP
     */
    suspend fun testConfig(): CommandResult {
        return termuxManager.executeCommand("php-fpm -t")
    }

    /**
     * Obtient la description d'une extension
     */
    private fun getExtensionDescription(extensionName: String): String {
        return when (extensionName) {
            "php-apache" -> "Module Apache pour PHP"
            "php-fpm" -> "FastCGI Process Manager"
            "php-pgsql" -> "Support PostgreSQL"
            "php-redis" -> "Support Redis"
            "php-curl" -> "Support cURL pour requetes HTTP"
            "php-gd" -> "Bibliotheque GD pour manipulation d'images"
            "php-mbstring" -> "Support multi-byte string"
            "php-xml" -> "Support XML"
            "php-zip" -> "Support archives ZIP"
            "php-sqlite" -> "Support SQLite"
            "php-intl" -> "Fonctions d'internationalisation"
            else -> "Extension PHP"
        }
    }
}
