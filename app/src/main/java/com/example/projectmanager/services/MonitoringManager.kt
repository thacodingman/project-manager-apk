package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonitoringManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()

    /**
     * Recupere les statistiques systeme
     */
    suspend fun getSystemStats() {
        val cpuUsage = getCPUUsage()
        val memoryUsage = getMemoryUsage()
        val storageUsage = getStorageUsage()
        val uptime = getUptime()

        _systemStats.value = SystemStats(
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            storageUsage = storageUsage,
            uptime = uptime
        )
    }

    /**
     * Recupere l'utilisation CPU
     */
    private suspend fun getCPUUsage(): Float {
        val result = termuxManager.executeCommand("""
            top -bn1 | grep "CPU:" | awk '{print $2}' | sed 's/%//' 2>/dev/null || echo "0"
        """.trimIndent())

        return result.output.trim().toFloatOrNull() ?: 0f
    }

    /**
     * Recupere l'utilisation memoire
     */
    private suspend fun getMemoryUsage(): MemoryInfo {
        val result = termuxManager.executeCommand("free -m")

        if (result.success) {
            val lines = result.output.lines()
            val memLine = lines.find { it.startsWith("Mem:") }

            memLine?.let {
                val parts = it.split("\\s+".toRegex())
                val total = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                val used = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                val free = parts.getOrNull(3)?.toLongOrNull() ?: 0L

                return MemoryInfo(
                    total = total,
                    used = used,
                    free = free,
                    percentage = if (total > 0) (used.toFloat() / total.toFloat() * 100) else 0f
                )
            }
        }

        return MemoryInfo(0, 0, 0, 0f)
    }

    /**
     * Recupere l'utilisation stockage
     */
    private suspend fun getStorageUsage(): StorageInfo {
        val result = termuxManager.executeCommand("df -h /data/data/com.termux/files/home")

        if (result.success) {
            val lines = result.output.lines()
            val dataLine = lines.getOrNull(1)

            dataLine?.let {
                val parts = it.split("\\s+".toRegex())
                val size = parts.getOrNull(1) ?: "0"
                val used = parts.getOrNull(2) ?: "0"
                val available = parts.getOrNull(3) ?: "0"
                val percentage = parts.getOrNull(4)?.replace("%", "")?.toFloatOrNull() ?: 0f

                return StorageInfo(
                    total = size,
                    used = used,
                    available = available,
                    percentage = percentage
                )
            }
        }

        return StorageInfo("0", "0", "0", 0f)
    }

    /**
     * Recupere l'uptime du systeme
     */
    private suspend fun getUptime(): String {
        val result = termuxManager.executeCommand("uptime -p 2>/dev/null || uptime")
        return result.output.trim()
    }

    /**
     * Liste les processus en cours
     */
    suspend fun getRunningProcesses(): CommandResult {
        return termuxManager.executeCommand("""
            ps aux | grep -E '(apache|nginx|mysql|postgres|node|sshd)' | grep -v grep
        """.trimIndent())
    }

    /**
     * Recupere les statistiques reseau
     */
    suspend fun getNetworkStats(): CommandResult {
        return termuxManager.executeCommand("""
            netstat -tuln 2>/dev/null | grep LISTEN || ss -tuln | grep LISTEN
        """.trimIndent())
    }

    /**
     * Verifie le statut de tous les services
     */
    suspend fun checkAllServices(): Map<String, Boolean> {
        val services = mapOf(
            "Apache" to "httpd",
            "Nginx" to "nginx",
            "MySQL" to "mysqld",
            "PostgreSQL" to "postgres",
            "SSH" to "sshd",
            "Node.js" to "node"
        )

        val statusMap = mutableMapOf<String, Boolean>()

        for ((name, process) in services) {
            val result = termuxManager.executeCommand("pgrep $process")
            statusMap[name] = result.success && result.output.isNotBlank()
        }

        return statusMap
    }
}

/**
 * Statistiques systeme
 */
data class SystemStats(
    val cpuUsage: Float = 0f,
    val memoryUsage: MemoryInfo = MemoryInfo(0, 0, 0, 0f),
    val storageUsage: StorageInfo = StorageInfo("0", "0", "0", 0f),
    val uptime: String = ""
)

/**
 * Informations memoire
 */
data class MemoryInfo(
    val total: Long,
    val used: Long,
    val free: Long,
    val percentage: Float
)

/**
 * Informations stockage
 */
data class StorageInfo(
    val total: String,
    val used: String,
    val available: String,
    val percentage: Float
)

