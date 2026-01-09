package com.example.projectmanager.models
/**
 * Service status
 */
enum class ServiceStatus {
    STOPPED,
    RUNNING,
    INSTALLING,
    UNKNOWN
}
/**
 * Service information
 */
data class ServiceInfo(
    val name: String,
    val status: ServiceStatus,
    val isInstalled: Boolean,
    val version: String = "",
    val port: Int = 0,
    val configPath: String = "",
    val logPath: String = ""
)
/**
 * Web configuration
 */
data class VirtualHost(
    val id: String,
    val serverName: String,
    val documentRoot: String,
    val port: Int = 8080,
    val enabled: Boolean = true,
    val configFile: String = ""
)
data class ServerBlock(
    val id: String,
    val serverName: String,
    val root: String,
    val port: Int = 8080,
    val enabled: Boolean = true,
    val configFile: String = "",
    val isProxy: Boolean = false,
    val proxyPass: String = ""
)
/**
 * PHP
 */
data class PHPExtension(
    val name: String,
    val displayName: String,
    val isInstalled: Boolean,
    val description: String
)
/**
 * Databases
 */
data class MySQLDatabase(val name: String, val size: String)
data class MySQLUser(val username: String, val host: String)
data class PostgreDatabase(val name: String, val size: String, val owner: String)
data class PostgreUser(val username: String, val isSuperuser: Boolean, val canCreateDB: Boolean)
/**
 * Strapi
 */
data class StrapiProject(val name: String, val path: String, val isRunning: Boolean, val port: Int)
/**
 * Monitoring & Backup
 */
data class BackupInfo(val name: String, val path: String, val size: String, val date: String)
data class SystemStats(
    val cpuUsage: Float = 0f,
    val memoryUsage: MemoryInfo = MemoryInfo(0, 0, 0, 0f),
    val storageUsage: StorageInfo = StorageInfo("0", "0", "0", 0f),
    val uptime: String = ""
)
data class MemoryInfo(val total: Long, val used: Long, val free: Long, val percentage: Float)
data class StorageInfo(val total: String, val used: String, val available: String, val percentage: Float)
/**
 * Templates & Deployments
 */
data class Template(
    val name: String,
    val path: String,
    val category: String,
    val description: String,
    val version: String = "1.0",
    val author: String = "",
    val createdDate: Long = System.currentTimeMillis(),
    val sourcePath: String = path,
    val createdAt: String = ""
)
/**
 * Additional template details
 */
data class TemplateDetails(
    val fileCount: Int,
    val size: String,
    val technologies: List<String>
)
data class Deployment(
    val name: String,
    val templateName: String,
    val webServer: String,
    val port: Int,
    val database: String,
    val path: String,
    val status: ServiceStatus = ServiceStatus.UNKNOWN
)
/**
 * Logs & DNS
 */
data class LogEntry(val timestamp: String, val level: String, val message: String)
data class DNSUpdateLog(
    val service: String,
    val domain: String,
    val success: Boolean,
    val message: String,
    val timestamp: Long
)
/**
 * SSH Connection
 */
data class SSHConnection(
    val name: String,
    val host: String,
    val port: Int,
    val username: String
)
/**
 * Command result
 */
data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    val success: Boolean get() = exitCode == 0
}