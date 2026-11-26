param([switch]$Reiniciar, [switch]$Todos)
$ErrorActionPreference = 'Stop'
$base = Join-Path $PSScriptRoot 'Perlita'
$servicios = [ordered]@{'service-auth'=8091; 'productos'=8082; 'categories'=8084; 'ventas'=8083; 'pedidos'=8085; 'gateway'=9090}
if ($Todos) { $servicios += [ordered]@{'materiales'=8081; 'proveedores'=8086; 'reportes'=8087; 'galeria'=8088; 'gastos'=8089; 'service_calendario'=8090} }
if (-not (Test-NetConnection 127.0.0.1 -Port 3306 -InformationLevel Quiet)) { throw 'Inicie MySQL desde XAMPP antes de continuar.' }
foreach ($nombre in $servicios.Keys) {
    $puerto = $servicios[$nombre]
    $directorio = Join-Path $base $nombre
    $procesos = @(Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($procesos.Count -gt 0) {
        if (-not $Reiniciar) { Write-Host "$nombre ya escucha en $puerto; se conserva."; continue }
        foreach ($procesoId in $procesos) {
            $proceso = Get-CimInstance Win32_Process -Filter "ProcessId=$procesoId"
            if ($proceso.Name -notmatch '^java(w)?\.exe$' -or $proceso.CommandLine -notlike "*$directorio*") { throw "Puerto $puerto ocupado por otro proceso. Cierre manualmente la terminal de $nombre y vuelva a ejecutar." }
            Stop-Process -Id $procesoId -Force
        }
    }
    Start-Process powershell.exe -WorkingDirectory $directorio -ArgumentList '-NoExit', '-Command', '.\mvnw.cmd spring-boot:run'
    Write-Host "Iniciando $nombre ($puerto)"
}
Write-Host 'Espere a que los servicios indiquen Started. Swagger: http://localhost:9090/swagger-ui.html'
