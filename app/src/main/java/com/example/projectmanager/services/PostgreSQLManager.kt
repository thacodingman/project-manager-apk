package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.*
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PostgreSQLManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _serviceInfo = MutableStateFlow(
        ServiceInfo(
            name = "PostgreSQL",
            status = ServiceStatus.UNKNOWN,
            isInstalled = false,
            port = 5432,
            configPath = "/data/data/com.termux/files/usr/var/lib/postgresql/data/postgresql.conf",
            logPath = "/data/data/com.termux/files/usr/var/lib/postgresql/data/log/"
        )
    )
    val serviceInfo: StateFlow<ServiceInfo> = _serviceInfo.asStateFlow()

    private val _databases = MutableStateFlow<List<PostgreDatabase>>(emptyList())
    val databases: StateFlow<List<PostgreDatabase>> = _databases.asStateFlow()

    private val _users = MutableStateFlow<List<PostgreUser>>(emptyList())
    val users: StateFlow<List<PostgreUser>> = _users.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /**
     * Verifie si PostgreSQL est installe
     */
    suspend fun checkInstallation(): CommandResult {
        val result = termuxManager.executeCommand("which psql")
        _serviceInfo.value = _serviceInfo.value.copy(
            isInstalled = result.success && result.output.isNotBlank()
        )
        return result
    }

    /**
     * Installe PostgreSQL
     */
    suspend fun install(): CommandResult {
        _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.INSTALLING)

        // Installer PostgreSQL
        val installResult = termuxManager.executeCommand("pkg install -y postgresql")
        if (!installResult.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.UNKNOWN)
            return installResult
        }

        // Initialiser le cluster de base de donnees
        val initResult = termuxManager.executeCommand(
            "mkdir -p \$PREFIX/var/lib/postgresql/data && " +
            "initdb -D \$PREFIX/var/lib/postgresql/data"
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
     * Configuration initiale de PostgreSQL
     */
    private suspend fun configureInitial() {
        // Configurer postgresql.conf pour ecouter sur localhost
        termuxManager.executeCommand("""
            echo "listen_addresses = 'localhost'" >> ${'$'}PREFIX/var/lib/postgresql/data/postgresql.conf
            echo "port = 5432" >> ${'$'}PREFIX/var/lib/postgresql/data/postgresql.conf
            echo "max_connections = 100" >> ${'$'}PREFIX/var/lib/postgresql/data/postgresql.conf
        """.trimIndent())

        // Configurer pg_hba.conf pour permettre les connexions locales
        termuxManager.executeCommand("""
            echo "local   all             all                                     trust" > ${'$'}PREFIX/var/lib/postgresql/data/pg_hba.conf
            echo "host    all             all             127.0.0.1/32            trust" >> ${'$'}PREFIX/var/lib/postgresql/data/pg_hba.conf
            echo "host    all             all             ::1/128                 trust" >> ${'$'}PREFIX/var/lib/postgresql/data/pg_hba.conf
        """.trimIndent())
    }

    /**
     * Demarre PostgreSQL
     */
    suspend fun start(): CommandResult {
        val result = termuxManager.executeCommand(
            "pg_ctl -D \$PREFIX/var/lib/postgresql/data -l \$PREFIX/var/lib/postgresql/data/log/postgres.log start"
        )
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Arrete PostgreSQL
     */
    suspend fun stop(): CommandResult {
        val result = termuxManager.executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql/data stop")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.STOPPED)
        }
        return result
    }

    /**
     * Redemarre PostgreSQL
     */
    suspend fun restart(): CommandResult {
        val result = termuxManager.executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql/data restart")
        if (result.success) {
            _serviceInfo.value = _serviceInfo.value.copy(status = ServiceStatus.RUNNING)
        }
        return result
    }

    /**
     * Verifie le statut de PostgreSQL
     */
    suspend fun checkStatus(): CommandResult {
        val result = termuxManager.executeCommand("pg_ctl -D \$PREFIX/var/lib/postgresql/data status")
        val isRunning = result.success && result.output.contains("server is running")
        _serviceInfo.value = _serviceInfo.value.copy(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED
        )
        return result
    }

    /**
     * Recupere la version de PostgreSQL
     */
    suspend fun getVersion(): CommandResult {
        val result = termuxManager.executeCommand("psql --version")
        if (result.success) {
            val version = result.output.substringAfter("psql (PostgreSQL) ").substringBefore("\n").trim()
            _serviceInfo.value = _serviceInfo.value.copy(version = version)
        }
        return result
    }

    /**
     * Liste toutes les bases de donnees
     */
    suspend fun loadDatabases() {
        val result = termuxManager.executeCommand(
            "psql -U postgres -c \"SELECT datname, pg_size_pretty(pg_database_size(datname)) as size FROM pg_database WHERE datistemplate = false;\" -t"
        )

        if (result.success) {
            val dbList = mutableListOf<PostgreDatabase>()
            result.output.lines().forEach { line ->
                if (line.trim().isNotBlank()) {
                    val parts = line.trim().split("|")
                    if (parts.size >= 2) {
                        dbList.add(
                            PostgreDatabase(
                                name = parts[0].trim(),
                                size = parts[1].trim(),
                                owner = "postgres"
                            )
                        )
                    }
                }
            }
            _databases.value = dbList
        }
    }

    /**
     * Cree une nouvelle base de donnees
     */
    suspend fun createDatabase(dbName: String, owner: String = "postgres"): CommandResult {
        val result = termuxManager.executeCommand(
            "psql -U postgres -c \"CREATE DATABASE $dbName OWNER $owner;\""
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
            "psql -U postgres -c \"DROP DATABASE IF EXISTS $dbName;\""
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
            "psql -U postgres -c \"SELECT usename, usesuper, usecreatedb FROM pg_user;\" -t"
        )

        if (result.success) {
            val userList = mutableListOf<PostgreUser>()
            result.output.lines().forEach { line ->
                if (line.trim().isNotBlank()) {
                    val parts = line.trim().split("|")
                    if (parts.size >= 3) {
                        userList.add(
                            PostgreUser(
                                username = parts[0].trim(),
                                isSuperuser = parts[1].trim() == "t",
                                canCreateDB = parts[2].trim() == "t"
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
        isSuperuser: Boolean = false,
        canCreateDB: Boolean = false
    ): CommandResult {
        val superuserFlag = if (isSuperuser) "SUPERUSER" else "NOSUPERUSER"
        val createdbFlag = if (canCreateDB) "CREATEDB" else "NOCREATEDB"

        val result = termuxManager.executeCommand(
            "psql -U postgres -c \"CREATE USER $username WITH PASSWORD '$password' $superuserFlag $createdbFlag;\""
        )
        if (result.success) {
            loadUsers()
        }
        return result
    }

    /**
     * Supprime un utilisateur
     */
    suspend fun deleteUser(username: String): CommandResult {
        val result = termuxManager.executeCommand(
            "psql -U postgres -c \"DROP USER IF EXISTS $username;\""
        )
        if (result.success) {
            loadUsers()
        }
        return result
    }

    /**
     * Change le mot de passe d'un utilisateur
     */
    suspend fun changePassword(username: String, newPassword: String): CommandResult {
        return termuxManager.executeCommand(
            "psql -U postgres -c \"ALTER USER $username WITH PASSWORD '$newPassword';\""
        )
    }

    /**
     * Accorde des permissions sur une base de donnees
     */
    suspend fun grantPermissions(username: String, dbName: String, privileges: String = "ALL"): CommandResult {
        return termuxManager.executeCommand(
            "psql -U postgres -c \"GRANT $privileges ON DATABASE $dbName TO $username;\""
        )
    }

    /**
     * Revoque des permissions
     */
    suspend fun revokePermissions(username: String, dbName: String, privileges: String = "ALL"): CommandResult {
        return termuxManager.executeCommand(
            "psql -U postgres -c \"REVOKE $privileges ON DATABASE $dbName FROM $username;\""
        )
    }

    /**
     * Execute une requete SQL
     */
    suspend fun executeQuery(dbName: String, query: String): CommandResult {
        return termuxManager.executeCommand(
            "psql -U postgres -d $dbName -c \"$query\""
        )
    }

    /**
     * Sauvegarde une base de donnees
     */
    suspend fun backupDatabase(dbName: String, backupPath: String): CommandResult {
        return termuxManager.executeCommand(
            "pg_dump -U postgres $dbName > $backupPath"
        )
    }

    /**
     * Restaure une base de donnees
     */
    suspend fun restoreDatabase(dbName: String, backupPath: String): CommandResult {
        return termuxManager.executeCommand(
            "psql -U postgres -d $dbName < $backupPath"
        )
    }

    /**
     * Recupere les logs PostgreSQL
     */
    suspend fun getLogs(): CommandResult {
        val result = termuxManager.executeCommand(
            "tail -n 100 \$PREFIX/var/lib/postgresql/data/log/postgres.log 2>/dev/null || echo 'Aucun log disponible'"
        )
        if (result.success) {
            _logs.value = result.output.lines()
        }
        return result
    }

    /**
     * Recupere les statistiques de la base de donnees
     */
    suspend fun getStats(dbName: String): CommandResult {
        return termuxManager.executeCommand(
            "psql -U postgres -d $dbName -c \"SELECT * FROM pg_stat_database WHERE datname = '$dbName';\""
        )
    }

    /**
     * Optimise une base de donnees (VACUUM)
     */
    suspend fun vacuumDatabase(dbName: String, full: Boolean = false): CommandResult {
        val vacuumCmd = if (full) "VACUUM FULL" else "VACUUM"
        return termuxManager.executeCommand(
            "psql -U postgres -d $dbName -c \"$vacuumCmd;\""
        )
    }
}

/**
 * Classe pour representer une base de donnees PostgreSQL
 */
data class PostgreDatabase(
    val name: String,
    val size: String,
    val owner: String
)

/**
 * Classe pour representer un utilisateur PostgreSQL
 */
data class PostgreUser(
    val username: String,
    val isSuperuser: Boolean,
    val canCreateDB: Boolean
)

