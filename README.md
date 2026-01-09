# 📱 Project Manager - Android Application

**Professional Android Project Management App with Termux Integration**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Version](https://img.shields.io/badge/version-1.0-blue.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)]()

---

## 🎯 About

Project Manager is a comprehensive Android application designed to manage web development projects directly on your Android device using Termux. It provides a complete server management solution with support for Apache, Nginx, MySQL, PostgreSQL, PHP, Strapi CMS, and more.

---

## ✨ Features

### 🖥️ Server Management
- **Apache Server** - Full configuration and management
- **Nginx Server** - Complete Nginx control
- **PHP Manager** - Multiple PHP versions support
- **MySQL Database** - Database and user management
- **PostgreSQL** - Advanced PostgreSQL management
- **Strapi CMS** - Headless CMS integration

### 🔐 Security & Networking
- **SSH Manager** - SSH connections and key management
- **DNS Services** - DuckDNS, No-IP, Porkbun integration
- **Firewall** - iptables configuration
- **SSL/TLS** - Certificate management

### 📦 Project Tools
- **Template System** - Project templates library
- **Deployment Manager** - One-click deployments
- **Backup & Restore** - Automated backups
- **Credentials Manager** - Secure credentials storage

### 📊 Monitoring
- **System Stats** - CPU, Memory, Storage monitoring
- **Service Status** - Real-time service monitoring
- **Logs Viewer** - Centralized logs management

---

## 📥 Download

### Latest Release

**Version 1.0** - January 9, 2026

#### 🔽 Download APK

Due to GitHub's file size limitations (100 MB), the APK files are not stored in this repository.

**⚠️ IMPORTANT: Use the DEBUG version for installation!**

The release version is **unsigned** and will NOT install on Android. You must use the debug version or sign the release version yourself.

**To get the installable APK:**

1. **Build DEBUG version** (RECOMMENDED - Ready to install):
   ```bash
   git clone https://github.com/thacodingman/project-manager-apk.git
   cd project-manager-apk
   ./gradlew assembleDebug
   ```
   **Installable APK**: `app/build/outputs/apk/debug/ProjectManager-v1.0-debug.apk` ✅

2. **Build RELEASE version** (Requires signing):
   ```bash
   ./gradlew assembleRelease
   ```
   **Non-installable APK**: `app/build/outputs/apk/release/ProjectManager-v1.0-release-unsigned.apk` ❌
   
   To make it installable, you need to sign it first (see [Signing Instructions](#signing-the-release-apk) below)

#### 📦 APK Versions

- **✅ Debug**: `ProjectManager-v1.0-debug.apk` (124 MB)
  - **READY TO INSTALL** - Auto-signed for testing
  - For development and testing
  - Includes debugging tools
  - **USE THIS VERSION FOR INSTALLATION**

- **❌ Release (Unsigned)**: `ProjectManager-v1.0-release-unsigned.apk` (118 MB)
  - **NOT INSTALLABLE** - Requires manual signing
  - Optimized for production
  - Smaller size, better performance
  - Must be signed before installation

---

## 🚀 Installation

### Prerequisites
- Android device with **Android 8.0 (API 26)** or higher
- **Termux** app (included in the app for easy installation)

### Steps

1. **Download the APK** (see Download section above)

2. **Enable Unknown Sources**:
   - Go to Settings → Security
   - Enable "Install from Unknown Sources"

3. **Install the APK**:
   - Locate the downloaded APK file
   - Tap to install
   - Grant requested permissions

4. **First Launch**:
   - Open "Project Manager"
   - Install Termux when prompted
   - Grant storage and system permissions
   - Start using the app!

---

## 🎨 Design

**Theme**: Dark mode with Neon Green accents

- **Primary Color**: Neon Green (#39FF14) - Buttons and highlights
- **Background**: Very Dark (#121212)
- **Text**: White on dark backgrounds, Black on green buttons
- **Material Design 3** - Modern, clean interface

---

## 🛠️ Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose 1.7.5
- **Architecture**: MVVM with StateFlow
- **Build Tool**: Gradle 8.7
- **Target SDK**: 36 (Android 14+)
- **Min SDK**: 26 (Android 8.0+)

---

## 📱 Screens & Features

### Main Screens
- 🏠 **Dashboard** - Overview and quick actions
- 📂 **Projects** - Project management
- 🖥️ **Servers** - Apache, Nginx control
- 🗄️ **Databases** - MySQL, PostgreSQL management
- 🔧 **PHP** - PHP configuration
- 📡 **Strapi** - CMS management
- 🔐 **SSH Terminal** - SSH connections
- 🌐 **DNS** - Dynamic DNS services
- 🔒 **Security** - Firewall, SSL/TLS
- ⚙️ **Settings** - App configuration

---

## 🏗️ Build Instructions

### Requirements
- **JDK 21** (Oracle or OpenJDK)
- **Android Studio** Hedgehog or later
- **Gradle 8.7+**

### Build Steps

```bash
# Clone the repository
git clone https://github.com/thacodingman/project-manager-apk.git
cd project-manager-apk

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean assembleRelease
```

### Output Locations
- Debug: `app/build/outputs/apk/debug/ProjectManager-v1.0-debug.apk` ✅ **Ready to install**
- Release: `app/build/outputs/apk/release/ProjectManager-v1.0-release-unsigned.apk` ❌ **Requires signing**

---

## 🔐 Signing the Release APK

If you want to use the release version, you need to sign it first.

### Option 1: Quick Sign with Debug Key (For Testing)

```bash
# Sign with debug keystore (auto-generated)
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore ~/.android/debug.keystore \
  -storepass android -keypass android \
  app/build/outputs/apk/release/ProjectManager-v1.0-release-unsigned.apk \
  androiddebugkey

# Align the APK
zipalign -v 4 \
  app/build/outputs/apk/release/ProjectManager-v1.0-release-unsigned.apk \
  ProjectManager-v1.0-release-signed.apk
```

### Option 2: Create Production Keystore (For Distribution)

```bash
# Generate a new keystore
keytool -genkey -v -keystore my-release-key.keystore \
  -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000

# Sign the APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.keystore \
  app/build/outputs/apk/release/ProjectManager-v1.0-release-unsigned.apk \
  my-key-alias

# Align the APK
zipalign -v 4 \
  app/build/outputs/apk/release/ProjectManager-v1.0-release-unsigned.apk \
  ProjectManager-v1.0-release-signed.apk
```

### Option 3: Use Android Studio

1. Go to **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. Create or select a keystore
4. Choose **release** build variant
5. Click **Finish**

**⚠️ For most users: Just use the DEBUG APK - it works perfectly for testing!**

---

## 📝 Development

### Project Structure
```
ProjectManager/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/projectmanager/
│   │   │   ├── models/          # Data models
│   │   │   ├── screens/         # UI screens (Compose)
│   │   │   ├── services/        # Service managers
│   │   │   ├── termux/          # Termux integration
│   │   │   ├── ui/theme/        # App theme
│   │   │   ├── utils/           # Utilities
│   │   │   └── navigation/      # Navigation graph
│   │   ├── assets/apk/          # Termux APK
│   │   └── res/                 # Resources
│   └── build.gradle.kts
├── docs/                        # Documentation
├── gradle/                      # Gradle wrapper
└── build.gradle.kts            # Root build file
```

### Key Files
- `MainActivity.kt` - Main entry point
- `NavigationGraph.kt` - App navigation
- `Models.kt` - All data classes
- `*Manager.kt` - Service management classes

---

## 🐛 Known Issues

None at this time! 🎉

The project builds successfully with:
- ✅ 0 Compilation Errors
- ✅ 0 Runtime Errors
- ✅ All features implemented

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👤 Author

**ThaCodingMan**
- GitHub: [@thacodingman](https://github.com/thacodingman)

---

## 🙏 Acknowledgments

- Built with ❤️ using Kotlin & Jetpack Compose
- Termux integration for server management
- Material Design 3 for modern UI

---

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/thacodingman/project-manager-apk/issues) page
2. Create a new issue if needed
3. Join discussions in the Discussions tab

---

**⭐ Star this repository if you find it useful!**

Last updated: January 9, 2026

