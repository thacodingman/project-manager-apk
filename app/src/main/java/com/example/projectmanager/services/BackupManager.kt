package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    /**
     * Cree un backup complet de la configuration
     */
    suspend fun createFullBackup(backupName: String): CommandResult {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupDir = "/data/data/com.termux/files/home/backups"
        val backupPath = "$backupDir/${backupName}_$timestamp"

        return termuxManager.executeCommand("""
            mkdir -p $backupPath
            
            # Backup des configurations
            cp -r /data/data/com.termux/files/home/.dns-config $backupPath/ 2>/dev/null || true
            cp -r /data/data/com.termux/files/home/.proxy-rules $backupPath/ 2>/dev/null || true
            cp -r /data/data/com.termux/files/home/.credentials $backupPath/ 2>/dev/null || true
            cp -r /data/data/com.termux/files/home/.ssh $backupPath/ 2>/dev/null || true
            
            # Backup des templates
            cp -r /data/data/com.termux/files/home/templates $backupPath/ 2>/dev/null || true
            
            # Backup des projets deployes
            cp -r /data/data/com.termux/files/home/deployments $backupPath/ 2>/dev/null || true
            
            # Creer une archive
            cd $backupDir
            tar -czf ${backupName}_$timestamp.tar.gz ${backupName}_$timestamp
            rm -rf ${backupName}_$timestamp
            
            echo "Backup cree: $backupPath.tar.gz"
        """.trimIndent())
    }

    /**
     * Liste tous les backups disponibles
     */
    suspend fun listBackups(): List<BackupInfo> {
        val backupDir = "/data/data/com.termux/files/home/backups"
        val result = termuxManager.executeCommand("""
            mkdir -p $backupDir
            ls -lh $backupDir/*.tar.gz 2>/dev/null | awk '{print $9"|"$5"|"$6" "$7" "$8}' || echo ''
        """.trimIndent())

        if (!result.success || result.output.isBlank()) {
            return emptyList()
        }

        return result.output.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split("|")
                val path = parts.getOrNull(0) ?: ""
                val size = parts.getOrNull(1) ?: "0"
                val date = parts.getOrNull(2) ?: ""

                BackupInfo(
                    name = path.substringAfterLast("/").removeSuffix(".tar.gz"),
                    path = path,
                    size = size,
                    date = date
                )
            }
    }

    /**
     * Restaure un backup
     */
    suspend fun restoreBackup(backupPath: String): CommandResult {
        return termuxManager.executeCommand("""
            cd /data/data/com.termux/files/home
            
            # Extraire l'archive
            tar -xzf $backupPath
            
            BACKUP_NAME=${'$'}(basename $backupPath .tar.gz)
            
            # Restaurer les configurations
            cp -r ${'$'}BACKUP_NAME/.dns-config /data/data/com.termux/files/home/ 2>/dev/null || true
            cp -r ${'$'}BACKUP_NAME/.proxy-rules /data/data/com.termux/files/home/ 2>/dev/null || true
            cp -r ${'$'}BACKUP_NAME/.credentials /data/data/com.termux/files/home/ 2>/dev/null || true
            cp -r ${'$'}BACKUP_NAME/.ssh /data/data/com.termux/files/home/ 2>/dev/null || true
            
            # Restaurer les templates
            cp -r ${'$'}BACKUP_NAME/templates /data/data/com.termux/files/home/ 2>/dev/null || true
            
            # Restaurer les deploiements
            cp -r ${'$'}BACKUP_NAME/deployments /data/data/com.termux/files/home/ 2>/dev/null || true
            
            # Nettoyer
            rm -rf ${'$'}BACKUP_NAME
            
            echo "Backup restaure avec succes"
        """.trimIndent())
    }

    /**
     * Supprime un backup
     */
    suspend fun deleteBackup(backupPath: String): CommandResult {
        return termuxManager.executeCommand("rm -f $backupPath")
    }
}
