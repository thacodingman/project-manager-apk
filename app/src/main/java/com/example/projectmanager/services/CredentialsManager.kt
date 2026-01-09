package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager

class CredentialsManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    /**
     * Credentials par defaut
     */
    val defaultCredentials = mapOf(
        "mysql_root_password" to "root",
        "postgresql_user" to "postgres",
        "postgresql_password" to "postgres",
        "ssh_port" to "8022",
        "apache_port" to "8080",
        "nginx_port" to "8081",
        "php_fpm_port" to "9000",
        "strapi_port" to "1337",
        "termux_user" to "u0_a"
    )

    /**
     * Recupere une credential
     */
    suspend fun getCredential(key: String): String {
        val configFile = "/data/data/com.termux/files/home/.credentials/$key"
        val result = termuxManager.executeCommand("cat $configFile 2>/dev/null || echo ''")

        return if (result.success && result.output.isNotBlank()) {
            result.output.trim()
        } else {
            defaultCredentials[key] ?: ""
        }
    }

    /**
     * Sauvegarde une credential
     */
    suspend fun saveCredential(key: String, value: String): CommandResult {
        val configDir = "/data/data/com.termux/files/home/.credentials"
        termuxManager.executeCommand("mkdir -p $configDir")

        return termuxManager.executeCommand("""
            echo '$value' > $configDir/$key
        """.trimIndent())
    }

    /**
     * Recupere toutes les credentials
     */
    suspend fun getAllCredentials(): Map<String, String> {
        val credentials = mutableMapOf<String, String>()

        for ((key, defaultValue) in defaultCredentials) {
            val value = getCredential(key)
            credentials[key] = value.ifBlank { defaultValue }
        }

        return credentials
    }

    /**
     * Reinitialise les credentials aux valeurs par defaut
     */
    suspend fun resetToDefaults(): CommandResult {
        return termuxManager.executeCommand(
            "rm -rf /data/data/com.termux/files/home/.credentials"
        )
    }
}

