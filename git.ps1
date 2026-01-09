
function Show-Menu {
    Clear-Host
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host "   PROJECT MANAGER - GIT AUTOMATION MENU      " -ForegroundColor Cyan
    Write-Host "==============================================" -ForegroundColor Cyan
    Write-Host "1. Voir le statut (git status)"
    Write-Host "2. TOUT SAUVEGARDER (Add + Commit + Sync)"
    Write-Host "3. Forcer la branche 'main' et lier l'URL origin"
    Write-Host "4. Récupérer les changements (Pull Rebase)"
    Write-Host "5. Envoyer vers GitHub (Push)"
    Write-Host "Q. Quitter"
    Write-Host "==============================================" -ForegroundColor Cyan
}

do {
    Show-Menu
    $choice = Read-Host "Choisissez une option"

    switch ($choice) {
        '1' {
            git status
            Pause
        }
        '2' {
            $msg = Read-Host "Message de commit (ex: correction bug)"
            if ([string]::IsNullOrWhiteSpace($msg)) { $msg = "Mise à jour automatique" }
            
            Write-Host "Ajout des fichiers..." -ForegroundColor Yellow
            git add .
            
            Write-Host "Enregistrement local (Commit)..." -ForegroundColor Yellow
            git commit -m $msg
            
            Write-Host "Synchronisation avec GitHub (Pull Rebase)..." -ForegroundColor Yellow
            git pull origin main --rebase
            
            Write-Host "Envoi vers GitHub (Push)..." -ForegroundColor Yellow
            git push origin main
            
            Write-Host "Terminé !" -ForegroundColor Green
            Pause
        }
        '3' {
            Write-Host "Configuration de la branche main..." -ForegroundColor Yellow
            git branch -M main
            Write-Host "Lien vers : https://github.com/thacodingman/project-manager-apk.git" -ForegroundColor Yellow
            git remote set-url origin https://github.com/thacodingman/project-manager-apk.git
            Write-Host "Ok !" -ForegroundColor Green
            Pause
        }
        '4' {
            git pull origin main --rebase
            Pause
        }
        '5' {
            git push origin main
            Pause
        }
    }
} while ($choice -ne 'q')
