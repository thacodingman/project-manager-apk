package com.example.projectmanager.services
import android.content.Context
import com.example.projectmanager.models.SSHConnection
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
     * Check if OpenSSH is installed
     */
    suspend fun checkSSHInstallation(): CommandResult {
        return termuxManager.executeCommand("which ssh")
    }
    /**
     * Install OpenSSH
     */
    suspend fun installSSH(): CommandResult {
        return termuxManager.executeCommand("pkg install -y openssh")
    }
    /**
     * Load all saved SSH connections
     */
    suspend fun loadConnections() {
        val sshDir = "/data/data/com.termux/files/home/.ssh-connections"
        termuxManager.executeCommand("mkdir -p $sshDir")
        val result = termuxManager.executeCommand("find $sshDir -name ''*.json'' -type f")
        if (result.success) {
            val connectionList = mutableListOf<SSHConnection>()
            result.output.lines().forEach { filePath ->
                if (filePath.trim().isNotBlank()) {
                    val contentResult = termuxManager.executeCommand("cat $filePath")
                    if (contentResult.success) {
                        val name = extractJsonField(contentResult.output, "name")
                        val host = extractJsonField(contentResult.output, "host")
                        val port = extractJsonField(contentResult.output, "port").toIntOrNull() ?: 22
                        val username = extractJsonField(contentResult.output, "username")
                        connectionList.add(
                            SSHConnection(
                                name = name,
                                host = host,
                                port = port,
                                username = username
                            )
                        )
                    }
                }
            }
            _connections.value = connectionList
        }
    }
    /**
     * Save a new SSH connection
     */
    suspend fun saveConnection(connection: SSHConnection): CommandResult {
        val sshDir = "/data/data/com.termux/files/home/.ssh-connections"
        val fileName = "${connection.name.replace(" ", "_")}.json"
        val jsonContent = """
            {
                "name": "${connection.name}",
                "host": "${connection.host}",
                "port": ${connection.port},
                "username": "${connection.username}"
            }
        """.trimIndent()
        val result = termuxManager.executeCommand("""
            cat > $sshDir/$fileName << ''EOF''
$jsonContent
EOF
        """.trimIndent())
        if (result.success) {
            loadConnections()
        }
        return result
    }
    /**
     * Delete a saved SSH connection
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
     * Connect via SSH
     */
    suspend fun connect(connection: SSHConnection, password: String = ""): CommandResult {
        val sshCommand = "sshpass -p ''$password'' ssh -p ${connection.port} ${connection.username}@${connection.host}"
        return termuxManager.executeCommand(sshCommand)
    }
    /**
     * Execute an SSH command on a connection
     */
    suspend fun executeSSHCommand(
        connection: SSHConnection,
        command: String,
        password: String = ""
    ): CommandResult {
        val sshCommand = "sshpass -p ''$password'' ssh -p ${connection.port} ${connection.username}@${connection.host} ''$command''"
        val result = termuxManager.executeCommand(sshCommand)
        if (result.success) {
            val history = _commandHistory.value.toMutableList()
            history.add(command)
            _commandHistory.value = history
        }
        return result
    }
    /**
     * Generate an SSH key pair
     */
    suspend fun generateSSHKey(
        keyName: String,
        keyType: String = "rsa",
        keyBits: Int = 4096
    ): CommandResult {
        val keyPath = "/data/data/com.termux/files/home/.ssh/$keyName"
        return termuxManager.executeCommand(
            "ssh-keygen -t $keyType -b $keyBits -f $keyPath -N ''''"
        )
    }
    /**
     * List all available SSH keys
     */
    suspend fun listSSHKeys(): CommandResult {
        return termuxManager.executeCommand("ls -la ~/.ssh/*.pub 2>/dev/null || echo ''No keys found''")
    }
    /**
     * Copy public key to remote server
     */
    suspend fun copySSHKey(
        keyPath: String,
        username: String,
        host: String,
        port: Int = 22,
        password: String
    ): CommandResult {
        return termuxManager.executeCommand(
            "sshpass -p ''$password'' ssh-copy-id -i $keyPath -p $port $username@$host"
        )
    }
    /**
     * Test SSH connection
     */
    suspend fun testConnection(connection: SSHConnection, password: String = ""): CommandResult {
        return executeSSHCommand(connection, "echo ''Connection successful''", password)
    }
    /**
     * Local SSH connection (localhost)
     */
    suspend fun connectLocal(username: String = "u0_a"): CommandResult {
        return termuxManager.executeCommand("ssh $username@localhost")
    }
    /**
     * Start SSH server (sshd)
     */
    suspend fun startSSHServer(): CommandResult {
        return termuxManager.executeCommand("sshd")
    }
    /**
     * Stop SSH server
     */
    suspend fun stopSSHServer(): CommandResult {
        return termuxManager.executeCommand("pkill sshd")
    }
    /**
     * Check SSH server status
     */
    suspend fun checkSSHServerStatus(): CommandResult {
        return termuxManager.executeCommand("pgrep sshd && echo ''Running'' || echo ''Stopped''")
    }
    /**
     * Get SSH server password
     */
    suspend fun getSSHServerPassword(): CommandResult {
        return termuxManager.executeCommand("cat ~/.ssh/server_password 2>/dev/null || echo ''Not set''")
    }
    /**
     * Set SSH server password
     */
    suspend fun setSSHServerPassword(password: String): CommandResult {
        return termuxManager.executeCommand("passwd")
    }
    /**
     * Load command history
     */
    suspend fun loadCommandHistory() {
        val historyFile = "/data/data/com.termux/files/home/.ssh_command_history"
        val result = termuxManager.executeCommand("cat $historyFile 2>/dev/null || echo ''''")
        if (result.success && result.output.isNotBlank()) {
            _commandHistory.value = result.output.lines().filter { it.isNotBlank() }
        }
    }
    /**
     * Save command history
     */
    suspend fun saveCommandHistory() {
        val historyFile = "/data/data/com.termux/files/home/.ssh_command_history"
        val content = _commandHistory.value.takeLast(100).joinToString("\n")
        termuxManager.executeCommand("""
            cat > $historyFile << ''EOF''
$content
EOF
        """.trimIndent())
    }
    /**
     * Simple JSON field extraction
     */
    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.getOrNull(1) ?: ""
    }
}
/**
 * SSH Key representation
 */
data class SSHKey(
    val name: String,
    val path: String,
    val publicKey: String,
    val type: String
)