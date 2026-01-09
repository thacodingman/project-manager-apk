package com.example.projectmanager.services

import android.content.Context
import com.example.projectmanager.models.Template
import com.example.projectmanager.models.TemplateDetails
import com.example.projectmanager.termux.CommandResult
import com.example.projectmanager.termux.TermuxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TemplateManager(private val context: Context) {
    private val termuxManager = TermuxManager(context)

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf(
        "Strapi CMS",
        "WordPress",
        "Laravel",
        "React",
        "Vue.js",
        "Next.js",
        "Django",
        "Flask",
        "Express.js",
        "Static HTML",
        "Autre"
    ))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    /**
     * Charge tous les templates disponibles
     */
    suspend fun loadTemplates() {
        val templateDir = "/data/data/com.termux/files/home/project-templates"

        // Creer le repertoire s'il n'existe pas
        termuxManager.executeCommand("mkdir -p $templateDir")

        // Lister les templates
        val result = termuxManager.executeCommand("find $templateDir -maxdepth 1 -type d ! -path $templateDir")

        if (result.success) {
            val templateList = mutableListOf<Template>()
            result.output.lines().forEach { path ->
                if (path.trim().isNotBlank()) {
                    val templateName = path.substringAfterLast("/")

                    // Lire les metadonnees du template
                    val metaResult = termuxManager.executeCommand("cat $path/template.json 2>/dev/null || echo '{}'")

                    // Parser basique du JSON (simplification)
                    val category = extractJsonField(metaResult.output, "category")
                    val description = extractJsonField(metaResult.output, "description")
                    val version = extractJsonField(metaResult.output, "version")
                    val author = extractJsonField(metaResult.output, "author")

                    templateList.add(
                        Template(
                            name = templateName,
                            path = path,
                            category = category.ifBlank { "Autre" },
                            description = description.ifBlank { "Aucune description" },
                            version = version.ifBlank { "1.0.0" },
                            author = author.ifBlank { "Anonyme" },
                            createdDate = System.currentTimeMillis()
                        )
                    )
                }
            }
            _templates.value = templateList
        }
    }

    /**
     * Cree un nouveau template a partir d'un projet existant
     */
    suspend fun createTemplate(
        name: String,
        sourcePath: String,
        category: String,
        description: String,
        version: String = "1.0.0",
        author: String = "User"
    ): CommandResult {
        val templateDir = "/data/data/com.termux/files/home/project-templates/$name"

        // Creer le repertoire du template
        val mkdirResult = termuxManager.executeCommand("mkdir -p $templateDir")
        if (!mkdirResult.success) return mkdirResult

        // Copier les fichiers sources
        val copyResult = termuxManager.executeCommand("cp -r $sourcePath/* $templateDir/")
        if (!copyResult.success) return copyResult

        // Creer le fichier de metadonnees
        val metadataJson = """
            {
                "name": "$name",
                "category": "$category",
                "description": "$description",
                "version": "$version",
                "author": "$author",
                "created": "${System.currentTimeMillis()}"
            }
        """.trimIndent()

        val metaResult = termuxManager.executeCommand("""
            cat > $templateDir/template.json << 'EOF'
$metadataJson
EOF
        """.trimIndent())

        if (metaResult.success) {
            loadTemplates()
        }

        return metaResult
    }

    /**
     * Supprime un template
     */
    suspend fun deleteTemplate(templateName: String): CommandResult {
        val templateDir = "/data/data/com.termux/files/home/project-templates/$templateName"
        val result = termuxManager.executeCommand("rm -rf $templateDir")

        if (result.success) {
            loadTemplates()
        }

        return result
    }

    /**
     * Exporte un template vers un fichier tar.gz
     */
    suspend fun exportTemplate(templateName: String, exportPath: String): CommandResult {
        val templateDir = "/data/data/com.termux/files/home/project-templates/$templateName"
        return termuxManager.executeCommand(
            "cd /data/data/com.termux/files/home/project-templates && tar -czf $exportPath/$templateName.tar.gz $templateName"
        )
    }

    /**
     * Importe un template depuis un fichier tar.gz
     */
    suspend fun importTemplate(archivePath: String): CommandResult {
        val templateDir = "/data/data/com.termux/files/home/project-templates"
        val result = termuxManager.executeCommand("cd $templateDir && tar -xzf $archivePath")

        if (result.success) {
            loadTemplates()
        }

        return result
    }

    /**
     * Duplique un template existant
     */
    suspend fun duplicateTemplate(templateName: String, newName: String): CommandResult {
        val sourceDir = "/data/data/com.termux/files/home/project-templates/$templateName"
        val destDir = "/data/data/com.termux/files/home/project-templates/$newName"

        val result = termuxManager.executeCommand("cp -r $sourceDir $destDir")

        if (result.success) {
            // Mettre a jour le nom dans le fichier de metadonnees
            termuxManager.executeCommand("""
                sed -i 's/"name": "$templateName"/"name": "$newName"/' $destDir/template.json
            """.trimIndent())
            loadTemplates()
        }

        return result
    }

    /**
     * Recupere les details d'un template
     */
    suspend fun getTemplateDetails(templateName: String): TemplateDetails? {
        val templateDir = "/data/data/com.termux/files/home/project-templates/$templateName"

        // Compter les fichiers
        val fileCountResult = termuxManager.executeCommand("find $templateDir -type f | wc -l")
        val fileCount = fileCountResult.output.trim().toIntOrNull() ?: 0

        // Calculer la taille
        val sizeResult = termuxManager.executeCommand("du -sh $templateDir | cut -f1")
        val size = sizeResult.output.trim()

        // Lister les technologies detectees
        val technologies = mutableListOf<String>()

        val checkFiles = mapOf(
            "package.json" to "Node.js",
            "composer.json" to "PHP/Composer",
            "requirements.txt" to "Python",
            "Gemfile" to "Ruby",
            "pom.xml" to "Java/Maven",
            "build.gradle" to "Java/Gradle",
            "index.html" to "HTML/Static"
        )

        checkFiles.forEach { (file, tech) ->
            val exists = termuxManager.executeCommand("test -f $templateDir/$file && echo 'yes' || echo 'no'")
            if (exists.output.trim() == "yes") {
                technologies.add(tech)
            }
        }

        return TemplateDetails(
            fileCount = fileCount,
            size = size,
            technologies = technologies
        )
    }

    /**
     * Extraction simple de champ JSON (sans bibliotheque)
     */
    private fun extractJsonField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"([^"]*)"""".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.getOrNull(1) ?: ""
    }
}


