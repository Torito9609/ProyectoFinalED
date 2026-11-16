#
# Levanta el backend (Spring Boot, puerto 8080) y el frontend (Vite, puerto 5173) con un solo comando.
# Es la versión para Windows de levantar.sh.
#
# Uso, desde PowerShell en la carpeta del backend:
#   powershell -ExecutionPolicy Bypass -File .\levantar.ps1
# Parar: Ctrl+C (detiene los dos).
#
# El frontend se busca en la carpeta hermana ..\ProyectoFinalED-frontend. Si está en otro lugar:
#   powershell -ExecutionPolicy Bypass -File .\levantar.ps1 -FrontendDir C:\ruta\al\frontend
#
# El backend escribe su salida en target\backend.log (y sus errores en target\backend-error.log);
# el frontend escribe en esta terminal.

param(
    [string]$FrontendDir = (Join-Path $PSScriptRoot '..\ProyectoFinalED-frontend')
)

$ErrorActionPreference = 'Stop'

$BackendDir = $PSScriptRoot
$LogBackend = Join-Path $BackendDir 'target\backend.log'
$LogErrores = Join-Path $BackendDir 'target\backend-error.log'
$PuertoBackend = 8080
$PuertoFrontend = 5173
$EsperaMaximaS = 90
$UrlApi = "http://localhost:$PuertoBackend/api/simulacion"

# Devuelve $true si algún programa ya escucha en el puerto.
function Test-PuertoOcupado([int]$Puerto) {
    $cliente = New-Object System.Net.Sockets.TcpClient
    try {
        $cliente.Connect('127.0.0.1', $Puerto)
        return $true
    } catch {
        return $false
    } finally {
        $cliente.Close()
    }
}

# Devuelve $true si la API responde.
function Test-ApiLista {
    try {
        Invoke-WebRequest -Uri $UrlApi -UseBasicParsing -TimeoutSec 2 | Out-Null
        return $true
    } catch {
        return $false
    }
}

# --- Comprobaciones antes de arrancar ------------------------------------------------------------

if (-not (Test-Path (Join-Path $FrontendDir 'package.json'))) {
    Write-Host "No encuentro el frontend en: $FrontendDir"
    Write-Host 'Clónalo junto a este repositorio o indica su ruta con -FrontendDir C:\ruta'
    exit 1
}
$FrontendDir = (Resolve-Path $FrontendDir).Path

foreach ($comando in 'java', 'node', 'npm') {
    if (-not (Get-Command $comando -ErrorAction SilentlyContinue)) {
        Write-Host "Falta '$comando' en el sistema."
        exit 1
    }
}

# Un puerto ocupado es la causa más común de que el backend termine con "exit code: 1".
foreach ($puerto in $PuertoBackend, $PuertoFrontend) {
    if (Test-PuertoOcupado $puerto) {
        Write-Host "El puerto $puerto ya está ocupado: quizá hay otro backend o frontend corriendo."
        Write-Host "Ciérralo y vuelve a intentar (para ver quién lo usa: netstat -ano | findstr :$puerto)."
        exit 1
    }
}

if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
    Write-Host 'Instalando las dependencias del frontend (solo la primera vez)...'
    Push-Location $FrontendDir
    try {
        & npm install
        if ($LASTEXITCODE -ne 0) { exit 1 }
    } finally {
        Pop-Location
    }
}

# --- Backend ---------------------------------------------------------------------------------------

New-Item -ItemType Directory -Force -Path (Join-Path $BackendDir 'target') | Out-Null
Write-Host 'Levantando el backend (salida en target\backend.log)...'
$backend = Start-Process -FilePath (Join-Path $BackendDir 'mvnw.cmd') `
    -ArgumentList '-q', 'spring-boot:run' `
    -WorkingDirectory $BackendDir `
    -RedirectStandardOutput $LogBackend -RedirectStandardError $LogErrores `
    -WindowStyle Hidden -PassThru

try {
    # Espera a que la API responda antes de abrir el frontend.
    $lista = $false
    for ($i = 0; $i -lt $EsperaMaximaS; $i++) {
        if (Test-ApiLista) { $lista = $true; break }
        if ($backend.HasExited) {
            Write-Host 'El backend terminó con error. Últimas líneas de target\backend.log:'
            Get-Content $LogBackend -Tail 20
            Get-Content $LogErrores -Tail 20
            exit 1
        }
        Start-Sleep -Seconds 1
    }
    if (-not $lista) {
        Write-Host "El backend no respondió en $EsperaMaximaS segundos. Revisa target\backend.log"
        exit 1
    }
    Write-Host "Backend listo en http://localhost:$PuertoBackend"

    # --- Frontend ----------------------------------------------------------------------------------

    Write-Host "Levantando el frontend en http://localhost:$PuertoFrontend (Ctrl+C para detener los dos)"
    Push-Location $FrontendDir
    try {
        # Se llama a Vite con node y no con "npm run dev": npm es un .cmd y, al pulsar Ctrl+C,
        # Windows preguntaría "¿Desea terminar el trabajo por lotes (S/N)?".
        & node 'node_modules\vite\bin\vite.js' --port $PuertoFrontend --strictPort
    } finally {
        Pop-Location
    }
} finally {
    # Al salir (Ctrl+C, error o fin del frontend) se detiene el backend. taskkill /T detiene también
    # los procesos hijos: mvnw.cmd lanza Maven, y Maven lanza la JVM de Spring Boot.
    Write-Host ''
    Write-Host 'Deteniendo el backend...'
    if (-not $backend.HasExited) {
        & taskkill /PID $backend.Id /T /F | Out-Null
    }
}
