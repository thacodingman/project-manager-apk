# 📋 TODO - Correction des Erreurs de Compilation

**Date**: 2026-01-09  
**Statut**: Stabilisation finale (95% terminé)

---

## ✅ ACTIONS EFFECTUÉES

### 1. Structure & Build
- [x] Correction du Build Gradle (SDK 36, Java 17).
- [x] Nettoyage de `PlaceholderScreens.kt` (suppression des doublons).
- [x] Centralisation de TOUS les modèles dans `Models.kt`.
- [x] Correction de `ProjectComponents.kt` (propriétés `Deployment` et `Template`).

### 2. Managers & Services
- [x] **MySQLManager.kt**, **DuckDNSManager.kt**, **PorkbunManager.kt**, **NoIPManager.kt** : Nettoyés et synchronisés.
- [x] **DeploymentManager.kt** : Correction des variables shell et des types.
- [x] Suppression des modèles locaux dans tous les services.

---

## 🚨 PRIORITÉS RESTANTES

### 1. Finalisation de Screens.kt
- [ ] Supprimer les imports ambigus (ex: `com.example.projectmanager.services.BackupInfo`).
- [ ] Spécifier explicitement les types dans les lambdas (ex: `items(backups) { backup: BackupInfo -> ... }`).
- [ ] Intégrer les composants de `ProjectComponents.kt` correctement.
- [ ] Corriger l'appel à `SSHManager.executeSSHCommand`.

### 2. Correction des Écrans de Sécurité & Paramètres
- [ ] **SecurityScreen.kt** : Supprimer le paramètre `scrollable` erroné.
- [ ] **SettingsScreen.kt** : Supprimer le paramètre `scrollable` erroné.

### 3. Validation Finale
- [ ] Vérifier le `NavigationGraph.kt`.
- [ ] Lancer un build final.

---

## 📊 ÉTAT DE LA COMPILATION (MIS À JOUR)

- **Managers**: ✅ OK
- **Modèles**: ✅ OK
- **Screens**: ~15 erreurs (Types et imports)
- **Navigation**: ✅ OK (après correction de Screens.kt)

**OBJECTIF : Build réussi à 100%.**

---

## ✅ CHECKLIST DE CORRECTION (Étape par étape)

- [x] **Étape 1** : Nettoyage des doublons (`PlaceholderScreens.kt`).
- [x] **Étape 2** : Correction de `ProjectComponents.kt`.
- [ ] **Étape 3** : Nettoyage de `SecurityScreen.kt` et `SettingsScreen.kt`.
- [ ] **Étape 4** : Finalisation de `Screens.kt`.
- [ ] **Étape 5** : Validation finale du `NavigationGraph.kt`.
