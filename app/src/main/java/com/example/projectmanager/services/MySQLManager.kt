package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MySQLManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _serviceInfo = MutableStateFlow(
        ServiceInfo(
            name = "MySQL/MariaDB",
            status = ServiceStatus.UNKNOWN,
            isInstalled = false,
            port = 3306,
            configPath = "/data/data/com.termux/files/usr/etc/my.cnf",
            logPath = "/data/data/com.termux/files/usr/var/log/mysql/"
        )
    )
    val serviceInfo: StateFlow<ServiceInfo> = _serviceInfo.asStateFlow()

    private val _databases = MutableStateFlow<List<MySQLDatabase>>(emptyList())
    val databases: StateFlow<List<MySQLDatabase>> = _databases.asStateFlow()

    private val _users = MutableStateFlow<List<MySQLUser>>(emptyList())
    val users: StateFlow<List<MySQLUser>> = _users.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /**
     * Verifie si MySQL/MariaDB est installe
     */
    suspend fun checkInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which mysql")
        _serviceInfo.value = _serviceInfo.value.copy(
            isInstalled = result.success && result.output.isNotBlank()
        )
        return result
    }

    /**
     * Installe MySQL/MariaDB
     */
    suspend fun install(): CommandResult {
        _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.INSTALLING)

        // Installer MariaDB (MySQL pour Termux)
        val installResult = termuxManager.executeCommand("pkg install -y mariadb")
        if (!installResult.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
            return installResult
        }

        // Initialiser la base de donnees
        val initResult = termuxManager.executeCommand(
            "mkdir -p ${'$'}PREFIX/var/lib/mysql && " +
            "mysql_install_db"
        )

        if (initResult.success) {
            _serviceInfo.value = _serviceInfo.value.copy(
                isInstalled = true,
                status = ServiceStatus.STOPPED
            )
            configureInitial()
            getVersion()
        } else {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
        }

        return initResult
    }

    /**
     * Configuration initiale de MySQL
     */
    private suspend fun configureInitial() {
        // Creer le fichier de configuration
        termuxManager.executeCommand("""
            mkdir -p ${'$'}PREFIX/etc
            cat > ${'$'}PREFIX/etc/my.cnf << 'EOF'
[mysqld]
bind-address = 127.0.0.1
port = 3306
datadir = ${'$'}PREFIX/var/lib/mysql
socket = ${'$'}PREFIX/tmp/mysqld.sock
pid-file = ${'$'}PREFIX/var/run/mysqld.pid

[client]
socket = ${'$'}PREFIX/tmp/mysqld.sock
EOF
        """.trimIndent())

        // Creer les repertoires necessaires
        termuxManager.executeCommand("""
            mkdir -p ${'$'}PREFIX/var/run
            mkdir -p ${'$'}PREFIX/tmp
            mkdir -p ${'$'}PREFIX/var/log/mysql
        """.trimIndent())
    }

    /**
     * Demarre MySQL
     */
    suspend fun start(): CommandResult {
        val result = termuxManager.executeCommand("mysqld_safe &")
        if (result.success) {
            // Attendre un peu que le serveur demarre
            kotlinx.coroutines.delay(2000)
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Arrete MySQL
     */
    suspend fun stop(): CommandResult {
        val result = termuxManager.executeCommand("mysqladmin -u root shutdown")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.STOPPED)
        }
        return result
    }

    /**
     * Redemarre MySQL
     */
    suspend fun restart(): CommandResult {
        stop()
        kotlinx.coroutines.delay(1000)
        return start()
    }

    /**
     * Verifie le statut de MySQL
     */
    suspend fun checkStatus(): CommandResult {
        val result = termuxManager.executeCommand("mysqladmin -u root ping")
        val isRunning = result.success && result.output.contains("alive")
        _serviceInfo.value = _serviceInfo.value.copy(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED
        )
        return result
    }

    /**
     * Recupere la version de MySQL
     */
    suspend fun getVersion(): CommandResult {
        val result = termuxManager.executeCommand("mysql --version")
        if (result.success) {
            val version = result.output.substringAfter("Ver ").substringBefore(" ").trim()
            _serviceInfo.value = _serviceInfo.value.copy(version = version)
        }
        return result
    }

    /**
     * Liste toutes les bases de donnees
     */
    suspend fun loadDatabases() {
        val result = termuxManager.executeCommand(
            "mysql -u root -e \"SELECT table_schema AS 'Database', ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' FROM information_schema.TABLES GROUP BY table_schema;\" -sN"
        )

        if (result.success) {
            val dbList = mutableListOf<MySQLDatabase>()
            result.output.lines().forEach { line ->
                if (line.trim().isNotBlank()) {
                    val parts = line.trim().split("\t")
                    if (parts.size >= 2) {
                        val dbName = parts[0].trim()
                        // Exclure les bases systeme de la liste
                        if (dbName !in listOf("information_schema", "performance_schema", "mysql", "sys")) {
                            dbList.add(
                                MySQLDatabase(
                                    name = dbName,
                                    size = "${parts[1].trim()} MB"
                                )
                            )
                        }
                    }
                }
            }
            _databases.value = dbList
        }
    }

    /**
     * Cree une nouvelle base de donnees
     */
    suspend fun createDatabase(dbName: String, charset: String = "utf8mb4"): CommandResult {
        val result = termuxManager.executeCommand(
            "mysql -u root -e \"CREATE DATABASE $dbName CHARACTER SET $charset COLLATE ${charset}_unicode_ci;\""
        )
        if (result.success) {
            loadDatabases()
        }
        return result
    }

    /**
     * Supprime une base de donnees
     */
    suspend fun deleteDatabase(dbName: String): CommandResult {
        val result = termuxManager.executeCommand(
            "mysql -u root -e \"DROP DATABASE IF EXISTS $dbName;\""
        )
        if (result.success) {
            loadDatabases()
        }
        return result
    }

    /**
     * Liste tous les utilisateurs
     */
    suspend fun loadUsers() {
        val result = termuxManager.executeCommand(
            "mysql -u root -e \"SELECT User, Host FROM mysql.user WHERE User != '';\" -sN"
        )

        if (result.success) {
            val userList = mutableListOf<MySQLUser>()
            result.output.lines().forEach { line ->
                if (line.trim().isNotBlank()) {
                    val parts = line.trim().split("\t")
                    if (parts.size >= 2) {
                        userList.add(
                            MySQLUser(
                                username = parts[0].trim(),
                                host = parts[1].trim()
                            )
                        )
                    }
                }
            }
            _users.value = userList
        }
    }

    /**
     * Cree un nouvel utilisateur
     */
    suspend fun createUser(
        username: String,
        password: String,
        host: String = "localhost"
    ): CommandResult {
        val result = termuxManager.executeCommand(
            "mysql -u root -e \"CREATE USER '$username'@'$host' IDENTIFIED BY '$password';\""
        )
        if (result.success) {
            loadUsers()
        }
        return result
    }

    /**
     * Supprime un utilisateur
     */
    suspend fun deleteUser(username: String, host: String = "localhost"): CommandResult {
        val result = termuxManager.executeCommand(
            "mysql -u root -e \"DROP USER IF EXISTS '$username'@'$host';\""
        )
        if (result.success) {
            loadUsers()
        }
        return result
    }

    /**
     * Change le mot de passe d'un utilisateur
     */
    suspend fun changePassword(username: String, newPassword: String, host: String = "localhost"): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root -e \"ALTER USER '$username'@'$host' IDENTIFIED BY '$newPassword';\""
        )
    }

    /**
     * Accorde tous les privileges sur une base de donnees
     */
    suspend fun grantAllPrivileges(username: String, dbName: String, host: String = "localhost"): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root -e \"GRANT ALL PRIVILEGES ON $dbName.* TO '$username'@'$host'; FLUSH PRIVILEGES;\""
        )
    }

    /**
     * Revoque tous les privileges
     */
    suspend fun revokeAllPrivileges(username: String, dbName: String, host: String = "localhost"): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root -e \"REVOKE ALL PRIVILEGES ON $dbName.* FROM '$username'@'$host'; FLUSH PRIVILEGES;\""
        )
    }

    /**
     * Execute une requete SQL
     */
    suspend fun executeQuery(dbName: String, query: String): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root -D $dbName -e \"$query\""
        )
    }

    /**
     * Sauvegarde une base de donnees
     */
    suspend fun backupDatabase(dbName: String, backupPath: String): CommandResult {
        return termuxManager.executeCommand(
            "mysqldump -u root $dbName > $backupPath"
        )
    }

    /**
     * Restaure une base de donnees
     */
    suspend fun restoreDatabase(dbName: String, backupPath: String): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root $dbName < $backupPath"
        )
    }

    /**
     * Recupere les logs MySQL
     */
    suspend fun getLogs(): CommandResult {
        val result = termuxManager.executeCommand(
            "tail -n 100 ${'$'}PREFIX/var/log/mysql/error.log 2>/dev/null || echo 'Aucun log disponible'"
        )
        if (result.success) {
            _logs.value = result.output.lines()
        }
        return result
    }

    /**
     * Recupere les variables de configuration
     */
    suspend fun getVariables(): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root -e \"SHOW VARIABLES LIKE '%version%';\""
        )
    }

    /**
     * Recupere le statut du serveur
     */
    suspend fun getServerStatus(): CommandResult {
        return termuxManager.executeCommand(
            "mysql -u root -e \"SHOW STATUS;\""
        )
    }

    /**
     * Optimise une base de donnees
     */
    suspend fun optimizeDatabase(dbName: String): CommandResult {
        return termuxManager.executeCommand(
            "mysqlcheck -u root -o $dbName"
        )
    }

    /**
     * Repare une base de donnees
     */
    suspend fun repairDatabase(dbName: String): CommandResult {
        return termuxManager.executeCommand(
            "mysqlcheck -u root -r $dbName"
        )
    }

    /**
     * Analyse une base de donnees
     */
    suspend fun analyzeDatabase(dbName: String): CommandResult {
        return termuxManager.executeCommand(
            "mysqlcheck -u root -a $dbName"
        )
    }
}
