package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class SecurityManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    /**
     * Chiffre une chaine avec AES
     */
    fun encryptString(data: String, key: String): String {
        try {
            val secretKey = SecretKeySpec(key.padEnd(16).substring(0, 16).toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Dechiffre une chaine avec AES
     */
    fun decryptString(encryptedData: String, key: String): String {
        try {
            val secretKey = SecretKeySpec(key.padEnd(16).substring(0, 16).toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes)
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Hash SHA-256 d'une chaine
     */
    fun hashSHA256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifie si le firewall (iptables) est disponible
     */
    suspend fun checkFirewallAvailable(): CommandResult {
        return termuxManager.executeCommand("which iptables")
    }

    /**
     * Installe iptables
     */
    suspend fun installFirewall(): CommandResult {
        return termuxManager.executeCommand("pkg install -y iptables")
    }

    /**
     * Liste les regles iptables actuelles
     */
    suspend fun listFirewallRules(): CommandResult {
        return termuxManager.executeCommand("iptables -L -n -v 2>/dev/null || echo 'Iptables non disponible'")
    }

    /**
     * Bloque un port specifique
     */
    suspend fun blockPort(port: Int): CommandResult {
        return termuxManager.executeCommand("""
            iptables -A INPUT -p tcp --dport $port -j DROP 2>&1 || echo 'Permissions root requises'
        """.trimIndent())
    }

    /**
     * Autorise un port specifique
     */
    suspend fun allowPort(port: Int): CommandResult {
        return termuxManager.executeCommand("""
            iptables -D INPUT -p tcp --dport $port -j DROP 2>&1 || echo 'Regle non trouvee ou permissions insuffisantes'
        """.trimIndent())
    }

    /**
     * Bloque une adresse IP
     */
    suspend fun blockIP(ip: String): CommandResult {
        return termuxManager.executeCommand("""
            iptables -A INPUT -s $ip -j DROP 2>&1 || echo 'Permissions root requises'
        """.trimIndent())
    }

    /**
     * Autorise une adresse IP
     */
    suspend fun allowIP(ip: String): CommandResult {
        return termuxManager.executeCommand("""
            iptables -D INPUT -s $ip -j DROP 2>&1 || echo 'Regle non trouvee ou permissions insuffisantes'
        """.trimIndent())
    }

    /**
     * Reinitialise toutes les regles iptables
     */
    suspend fun flushFirewallRules(): CommandResult {
        return termuxManager.executeCommand("""
            iptables -F 2>&1 || echo 'Permissions root requises'
        """.trimIndent())
    }

    /**
     * Sauvegarde les logs de securite
     */
    suspend fun logSecurityEvent(event: String, severity: String) {
        val timestamp = System.currentTimeMillis()
        val logEntry = "[$timestamp] [$severity] $event"

        termuxManager.executeCommand("""
            mkdir -p /data/data/com.termux/files/home/.security-logs
            echo '$logEntry' >> /data/data/com.termux/files/home/.security-logs/security.log
        """.trimIndent())
    }

    /**
     * Recupere les logs de securite
     */
    suspend fun getSecurityLogs(lines: Int = 50): CommandResult {
        return termuxManager.executeCommand("""
            tail -n $lines /data/data/com.termux/files/home/.security-logs/security.log 2>/dev/null || echo 'Aucun log'
        """.trimIndent())
    }

    /**
     * Efface les logs de securite
     */
    suspend fun clearSecurityLogs(): CommandResult {
        return termuxManager.executeCommand("""
            rm -f /data/data/com.termux/files/home/.security-logs/security.log
        """.trimIndent())
    }

    /**
     * Verifie les permissions d'un fichier
     */
    suspend fun checkFilePermissions(filePath: String): CommandResult {
        return termuxManager.executeCommand("ls -la $filePath")
    }

    /**
     * Change les permissions d'un fichier
     */
    suspend fun changeFilePermissions(filePath: String, permissions: String): CommandResult {
        return termuxManager.executeCommand("chmod $permissions $filePath")
    }

    /**
     * Genere un mot de passe aleatoire securise
     */
    fun generateSecurePassword(length: Int = 16): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-="
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Verifie la force d'un mot de passe
     */
    fun checkPasswordStrength(password: String): PasswordStrength {
        var score = 0

        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score < 3 -> PasswordStrength.WEAK
            score < 5 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }
}

/**
 * Force d'un mot de passe
 */
enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG
}

/**
 * evenement de securite
 */
data class SecurityEvent(
    val timestamp: Long,
    val severity: String,
    val message: String
)

/**
 * Regle de firewall
 */
data class FirewallRule(
    val type: String, // "port" ou "ip"
    val value: String,
    val action: String // "block" ou "allow"
)

