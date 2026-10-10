# Sincronizador Automático de Música por Cable USB para DaVE Player
# Detecta Infinix HOT 40i o cualquier teléfono Android conectado por MTP

Write-Host '==========================================================' -ForegroundColor Cyan
Write-Host '   DaVE Player -- Sincronizador USB de Canciones Moviles   ' -ForegroundColor Cyan
Write-Host '==========================================================' -ForegroundColor Cyan
Write-Host ''

$shell = New-Object -ComObject Shell.Application
$thisPc = $shell.NameSpace(17)
$phoneItem = $null

foreach ($it in $thisPc.Items()) {
    if ($it.Name -match 'Infinix|HOT|Android|Phone|Celular') {
        $phoneItem = $it
        break
    }
}

if (-not $phoneItem) {
    Write-Host '[!] No se detecto el celular conectado en modo MTP.' -ForegroundColor Yellow
    Write-Host '----------------------------------------------------------'
    Write-Host 'PASOS PARA CONECTAR:' -ForegroundColor White
    Write-Host ' 1. Conecta el cable USB de tu celular a la computadora.'
    Write-Host ' 2. Desbloquea tu celular y baja la barra de notificaciones.'
    Write-Host ' 3. Toca la notificacion "Cargando este dispositivo por USB".'
    Write-Host ' 4. Selecciona "Transferencia de archivos" o "MTP".'
    Write-Host ' 5. Vuelve a ejecutar este sincronizador.'
    Write-Host '----------------------------------------------------------'
    exit
}

Write-Host "[+] Celular detectado: $($phoneItem.Name)" -ForegroundColor Green

$phoneFolder = $phoneItem.GetFolder
$storage = $null

foreach ($it in $phoneFolder.Items()) {
    if ($it.Name -match 'interno|internal|almacenamiento|storage' -or $it.IsFolder) {
        $storage = $it.GetFolder
        break
    }
}

if (-not $storage) {
    Write-Host '[!] No se pudo acceder al almacenamiento interno del celular.' -ForegroundColor Red
    Write-Host 'Asegurate de desbloquear la pantalla de tu telefono.' -ForegroundColor Yellow
    exit
}

$destDir = Join-Path $PSScriptRoot 'desktop\src\music'
if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
}

function Get-Sub($parent, $name) {
    if (-not $parent) { return $null }
    foreach ($i in $parent.Items()) {
        if ($i.Name -eq $name) { return $i.GetFolder }
    }
    return $null
}

$foldersToScan = @()

$dl = Get-Sub $storage 'Download'
if ($dl) { 
    $foldersToScan += $dl
    $st = Get-Sub $dl 'snaptube'
    if ($st) {
        $foldersToScan += $st
        $stAudio = Get-Sub $st 'audio'
        if ($stAudio) { $foldersToScan += $stAudio }
        $stDl = Get-Sub $st 'download'
        if ($stDl) {
            $foldersToScan += $stDl
            $stDlAudio = Get-Sub $stDl 'SnapTube Audio'
            if ($stDlAudio) { $foldersToScan += $stDlAudio }
        }
    }
}

$music = Get-Sub $storage 'Music'
if ($music) { $foldersToScan += $music }

$stRoot = Get-Sub $storage 'snaptube'
if ($stRoot) { $foldersToScan += $stRoot }

Write-Host '[*] Buscando canciones nuevas en Download, Music y Snaptube...' -ForegroundColor Cyan

$copiedCount = 0
$destShell = $shell.NameSpace($destDir)

foreach ($f in $foldersToScan) {
    if (-not $f) { continue }
    foreach ($item in $f.Items()) {
        if ($item.IsFolder) { continue }
        $ext = [System.IO.Path]::GetExtension($item.Name).ToLower()
        if ($ext -in @('.mp3', '.m4a', '.wav', '.flac', '.aac', '.ogg', '.opus')) {
            $destPath = Join-Path $destDir $item.Name
            if (-not (Test-Path $destPath)) {
                Write-Host "  -> Copiando nueva cancion: $($item.Name)" -ForegroundColor Green
                try {
                    $destShell.CopyHere($item, 16)
                    $copiedCount++
                } catch {
                    Write-Host "     Error al copiar: $($item.Name)" -ForegroundColor Red
                }
            }
        }
    }
}

Write-Host ''
if ($copiedCount -gt 0) {
    Write-Host "FELICITACIONES: Se copiaron $copiedCount canciones nuevas a DaVE Player!" -ForegroundColor Green
} else {
    Write-Host 'INFORMACION: Todas las canciones de tu celular ya estan sincronizadas en la PC.' -ForegroundColor Cyan
}

Write-Host '==========================================================' -ForegroundColor Cyan
