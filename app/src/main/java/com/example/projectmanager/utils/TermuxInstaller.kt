package com.example.projectmanager.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class TermuxInstaller(private val context: Context) {
    
    /**
     * Check if Termux is installed
     * Supports multiple Termux variants
     */
    fun isTermuxInstalled(): Boolean {
        val termuxPackages = listOf(
            "com.termux",
            "com.termux.nightly",
            "com.termux.dev"
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
    private fun getInstalledTermuxPackage(): String? {
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
     * Verifie si l'APK Termux est disponible dans les assets
     * Note: L'APK est inclus par defaut dans ProjectManager
     */
    fun isTermuxApkAvailable(): Boolean {
        return try {
            context.assets.list("apk")?.contains("termux.apk") ?: false
        } catch (_: Exception) {
            false
        }
    }
    
    /**
     * Copie l'APK Termux des assets vers le stockage interne
     */
    private fun copyTermuxApkToStorage(): File? {
        return try {
            val apkFile = File(context.cacheDir, "termux.apk")
            
            context.assets.open("apk/termux.apk").use { input ->
                FileOutputStream(apkFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            apkFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lance l'installation de Termux depuis l'APK inclus
     * L'APK est deja package dans ProjectManager
     */
    fun installTermux(): Boolean {
        val apkFile = copyTermuxApkToStorage() ?: return false
        
        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get Termux version
     */
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
     * Obtient des informations sur l'APK Termux inclus
     */
    fun getIncludedApkInfo(): String {
        return "Termux F-Droid (officiel) - Inclus dans ProjectManager"
    }
}

