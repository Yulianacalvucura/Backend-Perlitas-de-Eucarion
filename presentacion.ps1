param([switch]$Todos)
$ErrorActionPreference = 'Stop'
$base = Join-Path $PSScriptRoot 'Perlita'
$frontend = Join-Path (Split-Path $PSScriptRoot -Parent) 'Frontend_PerlitasdeEucarion'
$servicios = [ordered]@{'service-auth'=8091; 'productos'=8082; 'categories'=8084; 'ventas'=8083; 'pedidos'=8085; 'gateway'=9090}
if ($Todos) { $servicios += [ordered]@{'materiales'=8081; 'proveedores'=8086; 'reportes'=8087; 'galeria'=8088; 'gastos'=8089; 'service_calendario'=8090} }
$procesos = @{}
$logs = Join-Path $env:TEMP ('Perlitas-demo-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))

function PuertoActivo([int]$puerto) {
    return [bool](Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue)
}
function Iniciar([string]$nombre, [string]$directorio, [string]$comando) {
    $p = Start-Process -FilePath $env:ComSpec -ArgumentList ('/d /c ' + $comando) -WorkingDirectory $directorio -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs "$nombre.log") -RedirectStandardError (Join-Path $logs "$nombre-error.log") -PassThru
    $procesos[$nombre] = $p
    Write-Host "Iniciando $nombre..."
}
function RevisarProceso([string]$nombre) {
    $p = $procesos[$nombre]
    $p.Refresh()
    if ($p.HasExited) { throw "$nombre se detuvo (codigo $($p.ExitCode)). Revise los logs en $logs" }
}
function EsperarPuerto([string]$nombre, [int]$puerto) {
    $limite = (Get-Date).AddMinutes(3)
    do {
        RevisarProceso $nombre
        if (PuertoActivo $puerto) { Write-Host "OK: $nombre ($puerto)" -ForegroundColor Green; return }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $limite)
    throw "$nombre no inicio en el puerto $puerto. Revise los logs en $logs"
}
function EsperarHttp([string]$url) {
    $limite = (Get-Date).AddMinutes(2)
    do {
        foreach ($nombre in $procesos.Keys) { RevisarProceso $nombre }
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) { Write-Host "HTTP 200: $url" -ForegroundColor Green; return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $limite)
    throw "No responde correctamente $url. Revise los logs en $logs"
}

if (-not (Test-NetConnection 127.0.0.1 -Port 3306 -InformationLevel Quiet)) { throw 'Inicie MySQL desde XAMPP.' }
if (-not (Test-Path (Join-Path $frontend 'node_modules'))) { throw "En $frontend ejecute npm install primero." }
foreach ($nombre in $servicios.Keys) {
    if (-not (Test-Path (Join-Path (Join-Path $base $nombre) 'mvnw.cmd'))) { throw "No se encontro Maven Wrapper de $nombre." }
}
$ocupados = @(@($servicios.Values) + @(5173) | Where-Object { PuertoActivo $_ })
if ($ocupados.Count -gt 0) { throw "Puertos ocupados: $($ocupados -join ', '). Detenga los servicios anteriores y Vite antes de iniciar esta demostracion." }
New-Item -ItemType Directory -Path $logs -Force | Out-Null
Write-Host "Logs: $logs"
try {
    foreach ($nombre in $servicios.Keys) {
        Iniciar $nombre (Join-Path $base $nombre) 'mvnw.cmd spring-boot:run'
    }
    foreach ($nombre in $servicios.Keys) { EsperarPuerto $nombre $servicios[$nombre] }
    EsperarHttp 'http://localhost:9090/api/v1/productos/tienda'
    EsperarHttp 'http://localhost:9090/api/v1/categorias/tienda'
    Iniciar 'frontend' $frontend 'npm.cmd run dev -- --strictPort'
    EsperarPuerto 'frontend' 5173
    EsperarHttp 'http://localhost:5173/api/v1/productos/tienda'
    EsperarHttp 'http://localhost:5173/tienda'
    Write-Host "`nDEMO LISTA" -ForegroundColor Green
    Write-Host 'Tienda: http://localhost:5173/tienda'
    Write-Host 'Personal: http://localhost:5173/login'
    Write-Host 'Swagger: http://localhost:9090/swagger-ui/index.html'
    Start-Process 'http://localhost:5173/tienda'
    Read-Host 'Presione Enter al finalizar para detener el frontend y los servicios iniciados' | Out-Null
} finally {
    foreach ($nombre in $procesos.Keys) {
        $p = $procesos[$nombre]
        try {
            $p.Refresh()
            if (-not $p.HasExited) { & taskkill.exe /PID $p.Id /T /F 2>&1 | Out-Null }
        } catch { Write-Warning "No se pudo detener $nombre. Compruebe sus procesos." }
    }
    Write-Host "Fin. Logs conservados en $logs"
    Write-Host 'XAMPP queda funcionando.'
}
