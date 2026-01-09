package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeploymentManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _deployments = MutableStateFlow<List<Deployment>>(emptyList())
    val deployments: StateFlow<List<Deployment>> = _deployments.asStateFlow()

    suspend fun loadDeployments() {
        val projectsDir = "/data/data/com.termux/files/home/deployed-projects"
        termuxManager.executeCommand("mkdir -p $projectsDir")
        val result = termuxManager.executeCommand("find $projectsDir -maxdepth 1 -type d ! -path $projectsDir")

        if (result.success) {
            val deploymentList = mutableListOf<Deployment>()
            result.output.lines().forEach { path ->
                if (path.trim().isNotBlank()) {
                    val projectName = path.substringAfterLast("/")
                    val metaResult = termuxManager.executeCommand("cat $path/deployment.json 2>/dev/null || echo '{}'")
                    val template = extractJsonField(metaResult.output, "template")
                    val port = extractJsonField(metaResult.output, "port").toIntOrNull() ?: 8080
                    val database = extractJsonField(metaResult.output, "database")
                    val webServer = extractJsonField(metaResult.output, "webServer")
                    val status = extractJsonField(metaResult.output, "status")

                    deploymentList.add(
                        Deployment(
                            name = projectName,
                            path = path,
                            templateName = template.ifBlank { "Inconnu" },
                            webServer = webServer.ifBlank { "None" },
                            port = port,
                            database = database,
                            status = when(status) {
                                "running" -> ServiceStatus.RUNNING
                                "stopped" -> ServiceStatus.STOPPED
                                else -> ServiceStatus.UNKNOWN
                            }
                        )
                    )
                }
            }
            _deployments.value = deploymentList
        }
    }

    suspend fun deployFromTemplate(
        projectName: String,
        templateName: String,
        domain: String,
        port: Int,
        createDatabase: Boolean = false,
        databaseType: String = "mysql",
        webServer: String = "apache"
    ): CommandResult {
        val projectDir = "/data/data/com.termux/files/home/deployed-projects/$projectName"
        val templateDir = "/data/data/com.termux/files/home/project-templates/$templateName"

        termuxManager.executeCommand("mkdir -p $projectDir")
        termuxManager.executeCommand("cp -r $templateDir/* $projectDir/")

        var databaseName = ""
        if (createDatabase) {
            databaseName = projectName.replace("-", "_")
            if (databaseType.lowercase() == "mysql") {
                termuxManager.executeCommand("mysql -u root -e \"CREATE DATABASE IF NOT EXISTS $databaseName;\"")
            }
        }

        if (webServer.lowercase() == "apache") configureApacheVHost(projectName, domain, port, projectDir)
        else configureNginxServerBlock(projectName, domain, port, projectDir)

        val metadataJson = """{"name":"$projectName","template":"$templateName","port":$port,"database":"$databaseName","webServer":"$webServer","status":"stopped"}"""
        val metaResult = termuxManager.executeCommand("echo '$metadataJson' > $projectDir/deployment.json")

        if (metaResult.success) loadDeployments()
        return metaResult
    }

    private suspend fun configureApacheVHost(projectName: String, domain: String, port: Int, projectDir: String) {
        val vhostConfig = """
        <VirtualHost *:$port>
            ServerName $domain
            DocumentRoot $projectDir
            ErrorLog ${'$'}{APACHE_LOG_DIR}/${projectName}_error.log
            CustomLog ${'$'}{APACHE_LOG_DIR}/${projectName}_access.log combined
        </VirtualHost>
        """.trimIndent()
        termuxManager.executeCommand("echo '$vhostConfig' > ${'$'}PREFIX/etc/apache2/sites-available/${projectName}.conf")
    }

    private suspend fun configureNginxServerBlock(projectName: String, domain: String, port: Int, projectDir: String) {
        val serverBlock = """
        server {
            listen $port;
            server_name $domain;
            root $projectDir;
            location / { try_files ${'$'}uri ${'$'}uri/ /index.php?${'$'}query_string; }
        }
        """.trimIndent()
        termuxManager.executeCommand("echo '$serverBlock' > ${'$'}PREFIX/etc/nginx/sites-available/${projectName}.conf")
    }

    suspend fun startDeployment(projectName: String, webServer: String): CommandResult {
        val cmd = if (webServer.lowercase() == "apache") "apachectl restart" else "nginx -s reload"
        val result = termuxManager.executeCommand(cmd)
        if (result.success) updateDeploymentStatus(projectName, "running")
        return result
    }

    suspend fun stopDeployment(projectName: String, webServer: String): CommandResult {
        updateDeploymentStatus(projectName, "stopped")
        return CommandResult(true, "Arrete", "", 0)
    }

    suspend fun deleteDeployment(projectName: String): CommandResult {
        val result = termuxManager.executeCommand("rm -rf /data/data/com.termux/files/home/deployed-projects/$projectName")
        if (result.success) loadDeployments()
        return result
    }

    suspend fun backupDeployment(projectName: String, backupPath: String): CommandResult {
        return termuxManager.executeCommand("tar -czf $backupPath/${projectName}.tar.gz -C /data/data/com.termux/files/home/deployed-projects $projectName")
    }

    private suspend fun updateDeploymentStatus(projectName: String, status: String) {
        val projectDir = "/data/data/com.termux/files/home/deployed-projects/$projectName"
        termuxManager.executeCommand("sed -i 's/\"status\": \"[^\"]*\"/\"status\": \"$status\"/' $projectDir/deployment.json")
        loadDeployments()
    }

    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        return pattern.find(json)?.groupValues?.getOrNull(1) ?: ""
    }
}
