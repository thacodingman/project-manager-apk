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

    private val termuxPath = "/data/data/com.termux/files"
    private val homePath = "$termuxPath/home"
    private val usrPath = "$termuxPath/usr"

    /**
     * Verifie si Termux est installe
     */
    fun isTermuxInstalled(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo("com.termux", 0)
            packageInfo != null
        } catch (e: Exception) {
            false
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
     * Verifie si un package est installe
     */
    suspend fun isPackageInstalled(packageName: String): CommandResult {
        return executePkgCommand("list-installed | grep $packageName")
    }

    /**
     * Installe un package
     */
    suspend fun installPackage(packageName: String): CommandResult {
        return executePkgCommand("install -y $packageName")
    }

    /**
     * Met a jour les packages
     */
    suspend fun updatePackages(): CommandResult {
        return executePkgCommand("update -y && pkg upgrade -y")
    }

    /**
     * Scripts d'installation pour les services
     */
    suspend fun installApache(): CommandResult {
        return installPackage("apache2")
    }

    suspend fun installNginx(): CommandResult {
        return installPackage("nginx")
    }

    suspend fun installPHP(): CommandResult {
        return installPackage("php")
    }

    suspend fun installPostgreSQL(): CommandResult {
        return installPackage("postgresql")
    }

    suspend fun installMySQL(): CommandResult {
        return installPackage("mariadb")
    }

    /**
     * Demarrer/Arreter les services
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

    suspend fun startPostgreSQL(): CommandResult {
        return executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql start")
    }

    suspend fun stopPostgreSQL(): CommandResult {
        return executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql stop")
    }

    suspend fun startMySQL(): CommandResult {
        return executeCommand("mysqld_safe &")
    }

    suspend fun stopMySQL(): CommandResult {
        return executeCommand("mysqladmin shutdown")
    }

    /**
     * Recupere les informations systeme
     */
    suspend fun getSystemInfo(): CommandResult {
        return executeCommand("uname -a")
    }

    suspend fun getDiskSpace(): CommandResult {
        return executeCommand("df -h")
    }

    suspend fun getMemoryInfo(): CommandResult {
        return executeCommand("free -h")
    }
}

