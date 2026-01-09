# 📁 Structure du projet ProjectManager

## 🗂️ Arborescence complète

```
ProjectManager/
│
├── 📄 Documentation (racine)
│   ├── README.md                    # Documentation générale
│   ├── CHANGELOG.md                 # Historique des changements
│   ├── BUILD-NOTES.md               # TODO list officielle
│   ├── PROJECT-STATUS.md            # Tableau de bord des progrès
│   ├── PHASE1-COMPLETE.md           # Documentation Phase 1
│   ├── PHASE2-COMPLETE.md           # Documentation Phase 2
│   ├── PHASE3-COMPLETE.md           # Documentation Phase 3
│   ├── PHASE4-COMPLETE.md           # Documentation Phase 4
│   ├── PHASE4-SUMMARY.md            # Résumé Phase 4
│   └── PROJECT-FILES.md             # Ce fichier
│
├── 📦 Configuration Gradle
│   ├── build.gradle.kts             # Configuration build projet
│   ├── settings.gradle.kts          # Configuration settings Gradle
│   ├── gradle.properties            # Propriétés Gradle
│   ├── gradlew                      # Gradle wrapper (Linux/Mac)
│   ├── gradlew.bat                  # Gradle wrapper (Windows)
│   ├── local.properties             # Propriétés locales
│   └── gradle/
│       ├── libs.versions.toml       # Versions des dépendances
│       └── wrapper/
│           ├── gradle-wrapper.jar
│           └── gradle-wrapper.properties
│
└── 📱 app/
    ├── build.gradle.kts             # Configuration build app
    ├── proguard-rules.pro           # Règles ProGuard
    │
    └── src/
        ├── 🧪 androidTest/
        │   └── java/com/example/projectmanager/
        │       └── ExampleInstrumentedTest.kt
        │
        ├── 🧪 test/
        │   └── java/com/example/projectmanager/
        │       └── ExampleUnitTest.kt
        │
        └── 📱 main/
            ├── AndroidManifest.xml  # Manifest avec permissions
            │
            ├── 🎨 res/
            │   ├── drawable/
            │   │   ├── ic_launcher_background.xml
            │   │   └── ic_launcher_foreground.xml
            │   ├── mipmap-*/        # Icônes launcher
            │   ├── values/
            │   │   ├── colors.xml
            │   │   ├── strings.xml
            │   │   └── themes.xml
            │   └── xml/
            │       ├── backup_rules.xml
            │       └── data_extraction_rules.xml
            │
            └── 💻 java/com/example/projectmanager/
                │
                ├── 📱 MainActivity.kt
                │   └── Point d'entrée de l'application
                │       ├── Drawer navigation
                │       ├── Top app bar
                │       └── Gestion du thème
                │
                ├── 🧭 navigation/
                │   ├── Screen.kt
                │   │   └── Définition des routes et items du menu
                │   │       ├── 11 écrans définis
                │   │       ├── Icônes associées
                │   │       └── Liste menuItems
                │   │
                │   └── NavigationGraph.kt
                │       └── Configuration de la navigation
                │           ├── NavHost
                │           └── Composables pour chaque écran
                │
                ├── 🎨 screens/
                │   └── Screens.kt (~2173 lignes)
                │       └── Tous les écrans de l'application
                │           ├── DashboardScreen
                │           ├── TermuxScreen (Phase 2) ✅
                │           ├── ApacheScreen (Phase 3) ✅
                │           ├── NginxScreen (Phase 3) ✅
                │           ├── PHPScreen (Phase 4) ✅
                │           ├── PostgreSQLScreen (placeholder)
                │           ├── MySQLScreen (placeholder)
                │           ├── MyTemplatesScreen (placeholder)
                │           ├── DeploymentsScreen (placeholder)
                │           ├── SSHTerminalScreen (placeholder)
                │           ├── SettingsScreen (placeholder)
                │           │
                │           └── Composants UI réutilisables :
                │               ├── ServiceHeader
                │               ├── InstallationSection
                │               ├── ServiceControlTab
                │               ├── VirtualHostsTab
                │               ├── ServerBlocksTab
                │               ├── LogsTab
                │               ├── PHPServiceControlTab
                │               ├── PHPExtensionsTab
                │               ├── PHPConfigTab
                │               └── Dialogs (VHost, ServerBlock)
                │
                ├── 📦 models/
                │   └── Models.kt
                │       └── Modèles de données
                │           ├── ServiceStatus (enum)
                │           ├── ServiceInfo (data class)
                │           ├── VirtualHost (data class)
                │           ├── ServerBlock (data class)
                │           └── LogEntry (data class)
                │
                ├── 🔧 services/
                │   ├── ApacheManager.kt (~1652 lignes)
                │   │   └── Gestionnaire Apache HTTP Server
                │   │       ├── Installation via Termux
                │   │       ├── Contrôle du service (start/stop/restart)
                │   │       ├── Gestion Virtual Hosts
                │   │       ├── Configuration automatique
                │   │       ├── Récupération des logs
                │   │       ├── Test de configuration
                │   │       └── StateFlow pour réactivité
                │   │
                │   ├── NginxManager.kt (~1500+ lignes)
                │   │   └── Gestionnaire Nginx
                │   │       ├── Installation via Termux
                │   │       ├── Contrôle du service (start/stop/reload)
                │   │       ├── Gestion Server Blocks
                │   │       ├── Support proxy inverse
                │   │       ├── Configuration automatique
                │   │       ├── Récupération des logs
                │   │       └── StateFlow pour réactivité
                │   │
                │   └── PHPManager.kt (~300 lignes)
                │       └── Gestionnaire PHP & PHP-FPM
                │           ├── Installation de PHP et PHP-FPM
                │           ├── Contrôle du service PHP-FPM
                │           ├── Gestion des extensions (11+)
                │           ├── Configuration php.ini interactive
                │           ├── Modification de directives
                │           ├── Récupération modules chargés
                │           ├── Consultation des logs
                │           └── StateFlow pour réactivité
                │
                ├── 🖥️ termux/
                │   └── TermuxManager.kt (~800 lignes)
                │       └── Gestionnaire de commandes Termux
                │           ├── Exécution de commandes shell
                │           ├── CommandResult (data class)
                │           ├── Installation de packages (pkg)
                │           ├── Scripts d'installation services
                │           ├── Démarrage/arrêt services
                │           ├── Informations système
                │           └── Gestion asynchrone avec Coroutines
                │
                ├── 🛠️ utils/
                │   └── PermissionsHelper.kt (~150 lignes)
                │       └── Gestionnaire de permissions Android
                │           ├── Vérification des permissions
                │           ├── Support Android 11+ (MANAGE_EXTERNAL_STORAGE)
                │           ├── Intents pour les paramètres
                │           └── Liste des permissions requises
                │
                └── 🎨 ui/theme/
                    ├── Color.kt
                    │   └── Couleurs du thème Material 3
                    ├── Theme.kt
                    │   └── Configuration du thème
                    └── Type.kt
                        └── Typographie
```

---

## 📊 Statistiques par type de fichier

### Code source Kotlin (.kt)
```
MainActivity.kt              ~150 lignes
Screen.kt                    ~40 lignes
NavigationGraph.kt           ~80 lignes
Screens.kt                   ~2173 lignes
Models.kt                    ~60 lignes
ApacheManager.kt             ~1652 lignes
NginxManager.kt              ~1500 lignes
PHPManager.kt                ~300 lignes
TermuxManager.kt             ~800 lignes
PermissionsHelper.kt         ~150 lignes
Color.kt                     ~30 lignes
Theme.kt                     ~80 lignes
Type.kt                      ~50 lignes
─────────────────────────────────────
TOTAL                        ~7065 lignes
```

### Documentation (.md)
```
README.md                    ~150 lignes
CHANGELOG.md                 ~220 lignes
BUILD-NOTES.md               ~200 lignes
PROJECT-STATUS.md            ~350 lignes
PHASE1-COMPLETE.md           ~100 lignes
PHASE2-COMPLETE.md           ~120 lignes
PHASE3-COMPLETE.md           ~200 lignes
PHASE4-COMPLETE.md           ~300 lignes
PHASE4-SUMMARY.md            ~250 lignes
PROJECT-FILES.md             ~400 lignes
─────────────────────────────────────
TOTAL                        ~2290 lignes
```

### Configuration (.xml, .kts, .toml, .properties)
```
AndroidManifest.xml          ~50 lignes
build.gradle.kts (projet)    ~30 lignes
build.gradle.kts (app)       ~100 lignes
settings.gradle.kts          ~20 lignes
libs.versions.toml           ~40 lignes
gradle.properties            ~20 lignes
colors.xml                   ~10 lignes
strings.xml                  ~20 lignes
themes.xml                   ~30 lignes
─────────────────────────────────────
TOTAL                        ~320 lignes
```

### TOTAL PROJET
```
Code Kotlin:     ~7065 lignes
Documentation:   ~2290 lignes
Configuration:   ~320 lignes
─────────────────────────────────────
TOTAL GLOBAL:    ~9675 lignes
```

---

## 🎯 Fichiers par phase

### Phase 1 : Configuration de base et UI
```
✅ MainActivity.kt
✅ navigation/Screen.kt
✅ navigation/NavigationGraph.kt
✅ screens/Screens.kt (structure de base)
✅ ui/theme/* (Color, Theme, Type)
✅ build.gradle.kts (ajout dépendances)
✅ libs.versions.toml
```

### Phase 2 : Intégration Termux
```
✅ termux/TermuxManager.kt
✅ utils/PermissionsHelper.kt
✅ screens/Screens.kt (TermuxScreen)
✅ AndroidManifest.xml (permissions)
```

### Phase 3 : Serveurs Web
```
✅ services/ApacheManager.kt
✅ services/NginxManager.kt
✅ models/Models.kt
✅ screens/Screens.kt (ApacheScreen, NginxScreen)
✅ screens/Screens.kt (composants UI réutilisables)
```

### Phase 4 : Langages et Runtime (PHP)
```
✅ services/PHPManager.kt
✅ screens/Screens.kt (PHPScreen)
✅ screens/Screens.kt (composants PHP)
```

---

## 🔑 Fichiers clés et leur rôle

### 🎯 MainActivity.kt
**Rôle** : Point d'entrée de l'application
- Initialisation du Drawer Navigation
- Configuration du Top App Bar
- Gestion du thème Material 3
- État de l'application (drawer ouvert/fermé)

### 🎯 TermuxManager.kt
**Rôle** : Interface avec Termux
- Exécution de commandes shell via Runtime.exec()
- Gestion asynchrone avec Coroutines
- Scripts d'installation pour tous les services
- Retour de CommandResult avec succès/erreur/sortie

### 🎯 ApacheManager.kt
**Rôle** : Gestion complète d'Apache
- Installation via pkg install apache2
- Création de Virtual Hosts
- Configuration automatique (ServerName, DocumentRoot, Logs)
- StateFlow pour réactivité UI

### 🎯 NginxManager.kt
**Rôle** : Gestion complète de Nginx
- Installation via pkg install nginx
- Création de Server Blocks
- Support proxy inverse (proxy_pass)
- StateFlow pour réactivité UI

### 🎯 PHPManager.kt
**Rôle** : Gestion de PHP et PHP-FPM
- Installation via pkg install php php-fpm
- Gestion de 11+ extensions PHP
- Configuration php.ini interactive
- StateFlow pour réactivité UI

### 🎯 Screens.kt
**Rôle** : Tous les écrans de l'application
- 11 écrans Compose
- Composants UI réutilisables
- Intégration avec les Managers
- Gestion des états avec remember et LaunchedEffect

### 🎯 Models.kt
**Rôle** : Modèles de données
- ServiceStatus (STOPPED, RUNNING, INSTALLING, UNKNOWN)
- ServiceInfo (informations service)
- VirtualHost (config Apache)
- ServerBlock (config Nginx)

---

## 📦 Dépendances principales

### build.gradle.kts (app)
```kotlin
// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.5")

// Icons
implementation("androidx.compose.material:material-icons-extended")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-runtime-ktx")
implementation("androidx.activity:activity-compose")
```

### libs.versions.toml
```toml
[versions]
compose = "1.5.4"
navigation = "2.7.5"
kotlin = "1.9.0"

[libraries]
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
```

---

## 🔜 Fichiers à créer (Phases futures)

### Phase 5 : Bases de données
```
À créer :
├── services/PostgreSQLManager.kt
├── services/MySQLManager.kt
└── screens/Screens.kt (PostgreSQLScreen, MySQLScreen)
```

### Phase 6 : Gestion de projets
```
À créer :
├── models/Template.kt
├── models/Project.kt
├── services/TemplateManager.kt
├── services/DeploymentManager.kt
└── screens/Screens.kt (MyTemplatesScreen, DeploymentsScreen)
```

### Phase 7 : Terminal SSH
```
À créer :
├── services/SSHManager.kt
├── models/SSHConnection.kt
└── screens/Screens.kt (SSHTerminalScreen)
```

### Phase 8 : Paramètres et DNS
```
À créer :
├── services/DuckDNSManager.kt
├── services/DynDNSManager.kt
├── services/NoIPManager.kt
├── models/DNSConfig.kt
└── screens/Screens.kt (SettingsScreen avec sous-sections)
```

---

## 📝 Notes importantes

### Bonnes pratiques suivies :
✅ **Architecture MVVM** - Séparation Manager / UI
✅ **StateFlow** - Réactivité pour l'UI
✅ **Coroutines** - Asynchrone sans bloquer l'UI
✅ **Material 3** - Design moderne et cohérent
✅ **Composables réutilisables** - DRY principle
✅ **Documentation complète** - Chaque phase documentée

### Structure modulaire :
- `services/` - Logique métier et intégration Termux
- `models/` - Modèles de données
- `screens/` - Interface utilisateur
- `navigation/` - Gestion de la navigation
- `utils/` - Utilitaires (permissions, etc.)
- `termux/` - Interface avec Termux

### Conventions de nommage :
- **Managers** : `ServiceNameManager.kt` (ex: ApacheManager)
- **Screens** : `ServiceNameScreen()` (ex: PHPScreen)
- **Models** : PascalCase (ex: ServiceInfo)
- **Composables** : PascalCase (ex: ServiceHeader)

---

**Dernière mise à jour** : 2026-01-08  
**Fichiers totaux** : ~30 fichiers
**Lignes de code totales** : ~9675 lignes

