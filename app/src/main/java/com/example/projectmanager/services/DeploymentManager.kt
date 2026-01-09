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

    /**
     * Charge tous les projets deployes
     */
    suspend fun loadDeployments() {
        val projectsDir = "/data/data/com.termux/files/home/deployed-projects"

        // Creer le repertoire s'il n'existe pas
        termuxManager.executeCommand("mkdir -p $projectsDir")

        // Lister les projets deployes
        val result = termuxManager.executeCommand("find $projectsDir -maxdepth 1 -type d ! -path $projectsDir")

        if (result.success) {
            val deploymentList = mutableListOf<Deployment>()
            result.output.lines().forEach { path ->
                if (path.trim().isNotBlank()) {
                    val projectName = path.substringAfterLast("/")

                    // Lire les metadonnees du deploiement
                    val metaResult = termuxManager.executeCommand("cat $path/deployment.json 2>/dev/null || echo '{}'")

                    val template = extractJsonField(metaResult.output, "template")
                    val domain = extractJsonField(metaResult.output, "domain")
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

    /**
     * Deploie un projet depuis un template
     */
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

        // Creer le repertoire du projet
        val mkdirResult = termuxManager.executeCommand("mkdir -p $projectDir")
        if (!mkdirResult.success) return mkdirResult

        // Copier les fichiers du template
        val copyResult = termuxManager.executeCommand("cp -r $templateDir/* $projectDir/")
        if (!copyResult.success) return copyResult

        // Creer la base de donnees si demande
        var databaseName = ""
        if (createDatabase) {
            databaseName = projectName.replace("-", "_")
            when (databaseType.lowercase()) {
                "mysql" -> {
                    termuxManager.executeCommand(
                        "mysql -u root -e \"CREATE DATABASE IF NOT EXISTS $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\""
                    )
                }
                "postgresql" -> {
                    termuxManager.executeCommand(
                        "psql -U postgres -c \"CREATE DATABASE $databaseName;\""
                    )
                }
            }
        }

        // Configurer le virtual host / server block
        when (webServer.lowercase()) {
            "apache" -> configureApacheVHost(projectName, domain, port, projectDir)
            "nginx" -> configureNginxServerBlock(projectName, domain, port, projectDir)
        }

        // Creer le fichier de metadonnees du deploiement
        val metadataJson = """
            {
                "name": "$projectName",
                "template": "$templateName",
                "domain": "$domain",
                "port": $port,
                "database": "$databaseName",
                "webServer": "$webServer",
                "status": "stopped",
                "deployed": "${System.currentTimeMillis()}"
            }
        """.trimIndent()

        val metaResult = termuxManager.executeCommand("""
            cat > $projectDir/deployment.json << 'EOF'
$metadataJson
EOF
        """.trimIndent())

        if (metaResult.success) {
            loadDeployments()
        }

        return metaResult
    }

    /**
     * Configure un Virtual Host Apache
     */
    private suspend fun configureApacheVHost(
        projectName: String,
        domain: String,
        port: Int,
        projectDir: String
    ) {
        val vhostConfig = """
<VirtualHost *:$port>
    ServerName $domain
    DocumentRoot $projectDir
    <Directory $projectDir>
        AllowOverride All
        Require all granted
    </Directory>
    ErrorLog ${'$'}{APACHE_LOG_DIR}/${projectName}_error.log
    CustomLog ${'$'}{APACHE_LOG_DIR}/${projectName}_access.log combined
</VirtualHost>
        """.trimIndent()

        termuxManager.executeCommand("""
            cat > ${'$'}PREFIX/etc/apache2/sites-available/${projectName}.conf << 'EOF'
$vhostConfig
EOF
        """.trimIndent())

        // Activer le site
        termuxManager.executeCommand("a2ensite ${projectName}.conf 2>/dev/null || ln -s ${'$'}PREFIX/etc/apache2/sites-available/${projectName}.conf ${'$'}PREFIX/etc/apache2/sites-enabled/")
    }

    /**
     * Configure un Server Block Nginx
     */
    private suspend fun configureNginxServerBlock(
        projectName: String,
        domain: String,
        port: Int,
        projectDir: String
    ) {
        val serverBlock = """
server {
    listen $port;
    server_name $domain;
    root $projectDir;
    index index.php index.html index.htm;
    
    location / {
        try_files ${'$'}uri ${'$'}uri/ /index.php?${'$'}query_string;
    }
    
    location ~ \.php${'$'} {
        fastcgi_pass 127.0.0.1:9000;
        fastcgi_index index.php;
        include fastcgi_params;
        fastcgi_param SCRIPT_FILENAME ${'$'}document_root${'$'}fastcgi_script_name;
    }
    
    error_log ${'$'}PREFIX/var/log/nginx/${projectName}_error.log;
    access_log ${'$'}PREFIX/var/log/nginx/${projectName}_access.log;
}
        """.trimIndent()

        termuxManager.executeCommand("""
            cat > ${'$'}PREFIX/etc/nginx/sites-available/${projectName}.conf << 'EOF'
$serverBlock
EOF
        """.trimIndent())

        // Activer le site
        termuxManager.executeCommand("ln -sf ${'$'}PREFIX/etc/nginx/sites-available/${projectName}.conf ${'$'}PREFIX/etc/nginx/sites-enabled/")
    }

    /**
     * Demarre un projet deploye
     */
    suspend fun startDeployment(projectName: String, webServer: String): CommandResult {
        // Redemarrer le serveur web
        val result = when (webServer.lowercase()) {
            "apache" -> termuxManager.executeCommand("apachectl restart")
            "nginx" -> termuxManager.executeCommand("nginx -s reload")
            else -> CommandResult(false, "", "Serveur web non reconnu", 1)
        }

        if (result.success) {
            updateDeploymentStatus(projectName, "running")
        }

        return result
    }

    /**
     * Arrete un projet deploye
     */
    suspend fun stopDeployment(projectName: String, webServer: String): CommandResult {
        // Desactiver le site
        when (webServer.lowercase()) {
            "apache" -> {
                termuxManager.executeCommand("a2dissite ${projectName}.conf 2>/dev/null || rm -f ${'$'}PREFIX/etc/apache2/sites-enabled/${projectName}.conf")
                termuxManager.executeCommand("apachectl restart")
            }
            "nginx" -> {
                termuxManager.executeCommand("rm -f ${'$'}PREFIX/etc/nginx/sites-enabled/${projectName}.conf")
                termuxManager.executeCommand("nginx -s reload")
            }
        }

        updateDeploymentStatus(projectName, "stopped")
        return CommandResult(true, "Projet arrete", "", 0)
    }

    /**
     * Supprime un projet deploye
     */
    suspend fun deleteDeployment(
        projectName: String,
        deleteDatabase: Boolean = false,
        databaseName: String = "",
        databaseType: String = "mysql"
    ): CommandResult {
        val projectDir = "/data/data/com.termux/files/home/deployed-projects/$projectName"

        // Supprimer les fichiers du projet
        val rmResult = termuxManager.executeCommand("rm -rf $projectDir")
        if (!rmResult.success) return rmResult

        // Supprimer la configuration du serveur web
        termuxManager.executeCommand("rm -f ${'$'}PREFIX/etc/apache2/sites-available/${projectName}.conf")
        termuxManager.executeCommand("rm -f ${'$'}PREFIX/etc/apache2/sites-enabled/${projectName}.conf")
        termuxManager.executeCommand("rm -f ${'$'}PREFIX/etc/nginx/sites-available/${projectName}.conf")
        termuxManager.executeCommand("rm -f ${'$'}PREFIX/etc/nginx/sites-enabled/${projectName}.conf")

        // Supprimer la base de donnees si demande
        if (deleteDatabase && databaseName.isNotBlank()) {
            when (databaseType.lowercase()) {
                "mysql" -> {
                    termuxManager.executeCommand("mysql -u root -e \"DROP DATABASE IF EXISTS $databaseName;\"")
                }
                "postgresql" -> {
                    termuxManager.executeCommand("psql -U postgres -c \"DROP DATABASE IF EXISTS $databaseName;\"")
                }
            }
        }

        loadDeployments()
        return CommandResult(true, "Projet supprime", "", 0)
    }

    /**
     * Sauvegarde un projet deploye
     */
    suspend fun backupDeployment(projectName: String, backupPath: String): CommandResult {
        val projectDir = "/data/data/com.termux/files/home/deployed-projects/$projectName"
        val timestamp = System.currentTimeMillis()

        return termuxManager.executeCommand(
            "cd /data/data/com.termux/files/home/deployed-projects && tar -czf $backupPath/${projectName}_${timestamp}.tar.gz $projectName"
        )
    }

    /**
     * Restaure un projet depuis une sauvegarde
     */
    suspend fun restoreDeployment(archivePath: String): CommandResult {
        val projectsDir = "/data/data/com.termux/files/home/deployed-projects"
        val result = termuxManager.executeCommand("cd $projectsDir && tar -xzf $archivePath")

        if (result.success) {
            loadDeployments()
        }

        return result
    }

    /**
     * Met a jour le statut d'un deploiement
     */
    private suspend fun updateDeploymentStatus(projectName: String, status: String) {
        val projectDir = "/data/data/com.termux/files/home/deployed-projects/$projectName"
        termuxManager.executeCommand("""
            sed -i 's/"status": "[^"]*"/"status": "$status"/' $projectDir/deployment.json
        """.trimIndent())
        loadDeployments()
    }

    /**
     * Clone un projet deploye
     */
    suspend fun cloneDeployment(
        sourceProjectName: String,
        newProjectName: String,
        newDomain: String,
        newPort: Int
    ): CommandResult {
        val sourceDir = "/data/data/com.termux/files/home/deployed-projects/$sourceProjectName"
        val destDir = "/data/data/com.termux/files/home/deployed-projects/$newProjectName"

        // Copier le projet
        val result = termuxManager.executeCommand("cp -r $sourceDir $destDir")

        if (result.success) {
            // Mettre a jour les metadonnees
            termuxManager.executeCommand("""
                sed -i 's/"name": "$sourceProjectName"/"name": "$newProjectName"/' $destDir/deployment.json
                sed -i 's/"domain": "[^"]*"/"domain": "$newDomain"/' $destDir/deployment.json
                sed -i 's/"port": [0-9]*/"port": $newPort/' $destDir/deployment.json
                sed -i 's/"status": "[^"]*"/"status": "stopped"/' $destDir/deployment.json
            """.trimIndent())

            loadDeployments()
        }

        return result
    }

    /**
     * Extraction simple de champ JSON
     */
    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.getOrNull(1) ?: ""
    }
}
