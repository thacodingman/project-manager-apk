package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StrapiManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _serviceInfo = MutableStateFlow(
        ServiceInfo(
            name = "Strapi",
            status = ServiceStatus.UNKNOWN,
            isInstalled = false,
            port = 1337, // Port par defaut Strapi
            configPath = "/data/data/com.termux/files/home/.strapi",
            logPath = "/data/data/com.termux/files/home/.npm/_logs"
        )
    )
    val serviceInfo: StateFlow<ServiceInfo> = _serviceInfo.asStateFlow()

    private val _projects = MutableStateFlow<List<StrapiProject>>(emptyList())
    val projects: StateFlow<List<StrapiProject>> = _projects.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /**
     * Verifie si Node.js et npm sont installes
     */
    suspend fun checkNodeInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which node")
        return result
    }

    /**
     * Verifie si Strapi CLI est installe
     */
    suspend fun checkInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which strapi")
        _serviceInfo.value = _serviceInfo.value.copy(
            isInstalled = result.success && result.output.isNotBlank()
        )
        return result
    }

    /**
     * Installe Node.js, npm et Strapi
     */
    suspend fun install(): CommandResult {
        _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.INSTALLING)

        // Installer Node.js et npm
        val nodeResult = termuxManager.executeCommand("pkg install -y nodejs")
        if (!nodeResult.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
            return nodeResult
        }

        // Installer Strapi CLI globalement
        val strapiResult = termuxManager.executeCommand("npm install -g create-strapi-app@latest")

        if (strapiResult.success) {
            _serviceInfo.value = _serviceInfo.value.copy(
                isInstalled = true,
                status = ServiceStatus.STOPPED
            )
            // Obtenir la version
            getVersion()
        } else {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
        }

        return strapiResult
    }

    /**
     * Cree un nouveau projet Strapi
     */
    suspend fun createProject(
        projectName: String,
        useQuickstart: Boolean = true,
        database: String = "sqlite"
    ): CommandResult {
        val command = if (useQuickstart) {
            "cd ~ && npx create-strapi-app@latest $projectName --quickstart"
        } else {
            "cd ~ && npx create-strapi-app@latest $projectName --dbclient=$database"
        }

        val result = termuxManager.executeCommand(command)
        if (result.success) {
            loadProjects()
        }
        return result
    }

    /**
     * Charge la liste des projets Strapi
     */
    suspend fun loadProjects() {
        val result = termuxManager.executeCommand(
            "find ~ -maxdepth 3 -type f -name 'strapi.js' -o -name '.strapirc' 2>/dev/null | sed 's|/[^/]*${'$'}||' | sort -u"
        )

        if (result.success) {
            val projectPaths = result.output.lines().filter { it.isNotBlank() }
            val projects = mutableListOf<StrapiProject>()

            for (path in projectPaths) {
                val name = path.substringAfterLast("/")
                val statusResult = termuxManager.executeCommand(
                    "ps aux | grep -v grep | grep 'strapi' | grep '$path'"
                )
                val isRunning = statusResult.success && statusResult.output.isNotBlank()

                // Lire le port depuis le fichier de config
                val portResult = termuxManager.executeCommand(
                    "grep -r 'port' $path/.env 2>/dev/null | grep -oP '\\d+' | head -1"
                )
                val port = portResult.output.trim().toIntOrNull() ?: 1337

                projects.add(
                    StrapiProject(
                        name = name,
                        path = path,
                        isRunning = isRunning,
                        port = port
                    )
                )
            }

            _projects.value = projects
        }
    }

    /**
     * Demarre un projet Strapi
     */
    suspend fun startProject(projectPath: String): CommandResult {
        val result = termuxManager.executeCommand(
            "cd $projectPath && nohup npm run develop > strapi.log 2>&1 &"
        )
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
            loadProjects()
        }
        return result
    }

    /**
     * Arrete un projet Strapi
     */
    suspend fun stopProject(projectPath: String): CommandResult {
        val result = termuxManager.executeCommand(
            "pkill -f 'strapi.*$projectPath'"
        )
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.STOPPED)
            loadProjects()
        }
        return result
    }

    /**
     * Build un projet Strapi pour la production
     */
    suspend fun buildProject(projectPath: String): CommandResult {
        return termuxManager.executeCommand("cd $projectPath && npm run build")
    }

    /**
     * Demarre un projet Strapi en mode production
     */
    suspend fun startProductionProject(projectPath: String): CommandResult {
        val result = termuxManager.executeCommand(
            "cd $projectPath && NODE_ENV=production nohup npm run start > strapi-prod.log 2>&1 &"
        )
        if (result.success) {
            loadProjects()
        }
        return result
    }

    /**
     * Recupere la version de Strapi
     */
    suspend fun getVersion(): CommandResult {
        val result = termuxManager.executeCommand("strapi version 2>/dev/null || npx strapi version")
        if (result.success) {
            val version = result.output.lines().firstOrNull { it.contains("@strapi") }
                ?.substringAfter("@")?.trim() ?: "Unknown"
            _serviceInfo.value = _serviceInfo.value.copy(version = version)
        }
        return result
    }

    /**
     * Verifie le statut global de Strapi
     */
    suspend fun checkStatus(): CommandResult {
        val result = termuxManager.executeCommand("pgrep -f strapi")
        val isRunning = result.success && result.output.isNotBlank()
        _serviceInfo.value = _serviceInfo.value.copy(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED
        )
        return result
    }

    /**
     * Recupere les logs d'un projet
     */
    suspend fun getLogs(projectPath: String): CommandResult {
        val result = termuxManager.executeCommand(
            "tail -n 100 $projectPath/strapi.log 2>/dev/null || echo 'Aucun log disponible'"
        )
        if (result.success) {
            _logs.value = result.output.lines()
        }
        return result
    }

    /**
     * Installe un plugin Strapi
     */
    suspend fun installPlugin(projectPath: String, pluginName: String): CommandResult {
        return termuxManager.executeCommand(
            "cd $projectPath && npm install @strapi/plugin-$pluginName"
        )
    }

    /**
     * Obtient les informations Node.js
     */
    suspend fun getNodeInfo(): Pair<String, String> {
        val nodeResult = termuxManager.executeCommand("node --version")
        val npmResult = termuxManager.executeCommand("npm --version")

        return Pair(
            nodeResult.output.trim().ifBlank { "Non installe" },
            npmResult.output.trim().ifBlank { "Non installe" }
        )
    }

    /**
     * Supprime un projet Strapi
     */
    suspend fun deleteProject(projectPath: String): CommandResult {
        // Arreter le projet d'abord
        stopProject(projectPath)

        // Supprimer le dossier
        val result = termuxManager.executeCommand("rm -rf $projectPath")
        if (result.success) {
            loadProjects()
        }
        return result
    }
}
