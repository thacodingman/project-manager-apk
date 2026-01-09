package com.example.projectmanager.models

/**
 * État d'un service
 */
enum class ServiceStatus {
    STOPPED,
    RUNNING,
    INSTALLING,
    UNKNOWN
}

/**
 * Informations sur un service
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
 * Configuration Web
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
 * Bases de données
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
    val description: String,
    val category: String,
    val sourcePath: String,
    val createdAt: String = ""
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
