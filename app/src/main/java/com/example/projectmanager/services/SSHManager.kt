package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SSHManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _connections = MutableStateFlow<List<SSHConnection>>(emptyList())
    val connections: StateFlow<List<SSHConnection>> = _connections.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    /**
     * Verifie si OpenSSH est installe
     */
    suspend fun checkSSHInstallation(): CommandResult {
        return termuxManager.executeCommand("which ssh")
    }

    /**
     * Installe OpenSSH
     */
    suspend fun installSSH(): CommandResult {
        return termuxManager.executeCommand("pkg install -y openssh")
    }

    /**
     * Charge toutes les connexions SSH sauvegardees
     */
    suspend fun loadConnections() {
        val sshDir = "/data/data/com.termux/files/home/.ssh-connections"

        // Creer le repertoire s'il n'existe pas
        termuxManager.executeCommand("mkdir -p $sshDir")

        // Lister les fichiers de connexion
        val result = termuxManager.executeCommand("find $sshDir -name '*.json' -type f")

        if (result.success) {
            val connectionList = mutableListOf<SSHConnection>()
            result.output.lines().forEach { filePath ->
                if (filePath.trim().isNotBlank()) {
                    // Lire le fichier de connexion
                    val contentResult = termuxManager.executeCommand("cat $filePath")
                    if (contentResult.success) {
                        val name = extractJsonField(contentResult.output, "name")
                        val host = extractJsonField(contentResult.output, "host")
                        val port = extractJsonField(contentResult.output, "port").toIntOrNull() ?: 22
                        val username = extractJsonField(contentResult.output, "username")
                        val useKey = extractJsonField(contentResult.output, "useKey") == "true"
                        val keyPath = extractJsonField(contentResult.output, "keyPath")

                        connectionList.add(
                            SSHConnection(
                                name = name,
                                host = host,
                                port = port,
                                username = username,
                                useKey = useKey,
                                keyPath = keyPath
                            )
                        )
                    }
                }
            }
            _connections.value = connectionList
        }
    }

    /**
     * Sauvegarde une nouvelle connexion SSH
     */
    suspend fun saveConnection(connection: SSHConnection): CommandResult {
        val sshDir = "/data/data/com.termux/files/home/.ssh-connections"
        val fileName = "${connection.name.replace(" ", "_")}.json"

        val jsonContent = """
            {
                "name": "${connection.name}",
                "host": "${connection.host}",
                "port": ${connection.port},
                "username": "${connection.username}",
                "useKey": ${connection.useKey},
                "keyPath": "${connection.keyPath}"
            }
        """.trimIndent()

        val result = termuxManager.executeCommand("""
            cat > $sshDir/$fileName << 'EOF'
$jsonContent
EOF
        """.trimIndent())

        if (result.success) {
            loadConnections()
        }

        return result
    }

    /**
     * Supprime une connexion SSH sauvegardee
     */
    suspend fun deleteConnection(connectionName: String): CommandResult {
        val sshDir = "/data/data/com.termux/files/home/.ssh-connections"
        val fileName = "${connectionName.replace(" ", "_")}.json"

        val result = termuxManager.executeCommand("rm -f $sshDir/$fileName")

        if (result.success) {
            loadConnections()
        }

        return result
    }

    /**
     * Se connecte via SSH
     */
    suspend fun connect(connection: SSHConnection, password: String = ""): CommandResult {
        val sshCommand = if (connection.useKey && connection.keyPath.isNotBlank()) {
            "ssh -i ${connection.keyPath} -p ${connection.port} ${connection.username}@${connection.host}"
        } else {
            "sshpass -p '$password' ssh -p ${connection.port} ${connection.username}@${connection.host}"
        }

        return termuxManager.executeCommand(sshCommand)
    }

    /**
     * Execute une commande SSH sur une connexion
     */
    suspend fun executeSSHCommand(
        connection: SSHConnection,
        command: String,
        password: String = ""
    ): CommandResult {
        val sshCommand = if (connection.useKey && connection.keyPath.isNotBlank()) {
            "ssh -i ${connection.keyPath} -p ${connection.port} ${connection.username}@${connection.host} '$command'"
        } else {
            "sshpass -p '$password' ssh -p ${connection.port} ${connection.username}@${connection.host} '$command'"
        }

        val result = termuxManager.executeCommand(sshCommand)

        // Ajouter a l'historique
        if (result.success) {
            val history = _commandHistory.value.toMutableList()
            history.add(command)
            _commandHistory.value = history
        }

        return result
    }

    /**
     * Genere une paire de cles SSH
     */
    suspend fun generateSSHKey(
        keyName: String,
        keyType: String = "rsa",
        keyBits: Int = 4096
    ): CommandResult {
        val keyPath = "/data/data/com.termux/files/home/.ssh/$keyName"

        return termuxManager.executeCommand(
            "ssh-keygen -t $keyType -b $keyBits -f $keyPath -N ''"
        )
    }

    /**
     * Liste toutes les cles SSH disponibles
     */
    suspend fun listSSHKeys(): CommandResult {
        return termuxManager.executeCommand("ls -la ~/.ssh/*.pub 2>/dev/null || echo 'Aucune cle trouvee'")
    }

    /**
     * Copie la cle publique sur un serveur distant
     */
    suspend fun copySSSHKey(
        keyPath: String,
        username: String,
        host: String,
        port: Int = 22,
        password: String
    ): CommandResult {
        return termuxManager.executeCommand(
            "sshpass -p '$password' ssh-copy-id -i $keyPath -p $port $username@$host"
        )
    }

    /**
     * Teste la connexion SSH
     */
    suspend fun testConnection(connection: SSHConnection, password: String = ""): CommandResult {
        return executeSSHCommand(connection, "echo 'Connection successful'", password)
    }

    /**
     * Connexion SSH locale (localhost)
     */
    suspend fun connectLocal(username: String = "u0_a"): CommandResult {
        return termuxManager.executeCommand("ssh $username@localhost")
    }

    /**
     * Demarre le serveur SSH (sshd)
     */
    suspend fun startSSHServer(): CommandResult {
        return termuxManager.executeCommand("sshd")
    }

    /**
     * Arrete le serveur SSH
     */
    suspend fun stopSSHServer(): CommandResult {
        return termuxManager.executeCommand("pkill sshd")
    }

    /**
     * Verifie le statut du serveur SSH
     */
    suspend fun checkSSHServerStatus(): CommandResult {
        return termuxManager.executeCommand("pgrep sshd && echo 'Running' || echo 'Stopped'")
    }

    /**
     * Recupere le mot de passe SSH du serveur local
     */
    suspend fun getSSHServerPassword(): CommandResult {
        return termuxManager.executeCommand("cat ~/.ssh/server_password 2>/dev/null || echo 'Non defini'")
    }

    /**
     * Definit le mot de passe du serveur SSH
     */
    suspend fun setSSHServerPassword(password: String): CommandResult {
        return termuxManager.executeCommand("passwd")
    }

    /**
     * Charge l'historique des commandes
     */
    suspend fun loadCommandHistory() {
        val historyFile = "/data/data/com.termux/files/home/.ssh_command_history"
        val result = termuxManager.executeCommand("cat $historyFile 2>/dev/null || echo ''")

        if (result.success && result.output.isNotBlank()) {
            _commandHistory.value = result.output.lines().filter { it.isNotBlank() }
        }
    }

    /**
     * Sauvegarde l'historique des commandes
     */
    suspend fun saveCommandHistory() {
        val historyFile = "/data/data/com.termux/files/home/.ssh_command_history"
        val content = _commandHistory.value.takeLast(100).joinToString("\n")

        termuxManager.executeCommand("""
            cat > $historyFile << 'EOF'
$content
EOF
        """.trimIndent())
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

/**
 * Classe pour representer une connexion SSH
 */
data class SSHConnection(
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val useKey: Boolean = false,
    val keyPath: String = ""
)

/**
 * Classe pour representer une cle SSH
 */
data class SSHKey(
    val name: String,
    val path: String,
    val publicKey: String,
    val type: String
)

