package com.example.projectmanager.termux

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val success: Boolean = exitCode == 0
)

class TermuxManager(private val context: Context) {

    @Suppress("unused")
    private val termuxPath = "/data/data/com.termux/files"

    /**
     * Check if Termux is installed
     * Supports multiple Termux variants: F-Droid, GitHub, Play Store
     */
    @Suppress("unused")
    fun isTermuxInstalled(): Boolean {
        val termuxPackages = listOf(
            "com.termux",              // F-Droid/GitHub official
            "com.termux.nightly",      // Nightly builds
            "com.termux.dev"           // Development builds
        )

        return termuxPackages.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Get installed Termux package name
     */
    fun getInstalledTermuxPackage(): String? {
        val termuxPackages = listOf(
            "com.termux",
            "com.termux.nightly",
            "com.termux.dev"
        )

        return termuxPackages.firstOrNull { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Get Termux version
     */
    @Suppress("unused")
    fun getTermuxVersion(): String? {
        val packageName = getInstalledTermuxPackage() ?: return null
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Execute une commande shell
     */
    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))

            val output = StringBuilder()
            val error = StringBuilder()

            // Lire la sortie standard
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { line ->
                    output.append(line).append("\n")
                }
            }

            // Lire la sortie d'erreur
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.forEachLine { line ->
                    error.append(line).append("\n")
                }
            }

            val exitCode = process.waitFor()

            CommandResult(
                exitCode = exitCode,
                output = output.toString(),
                error = error.toString()
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = -1,
                output = "",
                error = "Erreur d'execution: ${e.message}"
            )
        }
    }

    /**
     * Execute une commande Termux via pkg
     */
    suspend fun executePkgCommand(command: String): CommandResult {
        return executeCommand("pkg $command")
    }

    /**
     * Check if a package is installed
     */
    @Suppress("unused")
    suspend fun isPackageInstalled(packageName: String): CommandResult {
        return executePkgCommand("list-installed | grep $packageName")
    }

    /**
     * Install a package
     */
    suspend fun installPackage(packageName: String): CommandResult {
        return executePkgCommand("install -y $packageName")
    }

    /**
     * Update packages
     */
    @Suppress("unused")
    suspend fun updatePackages(): CommandResult {
        return executePkgCommand("update -y && pkg upgrade -y")
    }

    /**
     * Installation scripts for services
     */
    suspend fun installApache(): CommandResult {
        return installPackage("apache2")
    }

    suspend fun installNginx(): CommandResult {
        return installPackage("nginx")
    }

    @Suppress("unused")
    suspend fun installPHP(): CommandResult {
        return installPackage("php")
    }

    @Suppress("unused")
    suspend fun installPostgreSQL(): CommandResult {
        return installPackage("postgresql")
    }

    @Suppress("unused")
    suspend fun installMySQL(): CommandResult {
        return installPackage("mariadb")
    }

    /**
     * Start/Stop services
     */
    suspend fun startApache(): CommandResult {
        return executeCommand("apachectl start")
    }

    suspend fun stopApache(): CommandResult {
        return executeCommand("apachectl stop")
    }

    suspend fun startNginx(): CommandResult {
        return executeCommand("nginx")
    }

    suspend fun stopNginx(): CommandResult {
        return executeCommand("nginx -s stop")
    }

    @Suppress("unused")
    suspend fun startPostgreSQL(): CommandResult {
        return executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql start")
    }

    @Suppress("unused")
    suspend fun stopPostgreSQL(): CommandResult {
        return executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql stop")
    }

    @Suppress("unused")
    suspend fun startMySQL(): CommandResult {
        return executeCommand("mysqld_safe &")
    }

    @Suppress("unused")
    suspend fun stopMySQL(): CommandResult {
        return executeCommand("mysqladmin shutdown")
    }

    /**
     * Get system information
     */
    @Suppress("unused")
    suspend fun getSystemInfo(): CommandResult {
        return executeCommand("uname -a")
    }

    @Suppress("unused")
    suspend fun getDiskSpace(): CommandResult {
        return executeCommand("df -h")
    }

    @Suppress("unused")
    suspend fun getMemoryInfo(): CommandResult {
        return executeCommand("free -h")
    }
}

