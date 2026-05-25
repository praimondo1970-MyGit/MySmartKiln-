# Script para verificar estado WiFi del ESP32
$port = "COM7"
$baud = 115200

Write-Host "Conectando al ESP32 en $port..."
Write-Host "Esperando 15 segundos para capturar logs..."
Write-Host ""

# Esperar un poco antes de conectar
Start-Sleep -Seconds 2

# Capturar logs por 15 segundos
$startTime = Get-Date
$logs = @()

try {
    # Usar platformio device monitor
    $process = Start-Process -FilePath "$env:USERPROFILE\.platformio\penv\Scripts\platformio.exe" `
        -ArgumentList "device", "monitor", "-e", "esp32-s3-devkitc-1", "--baud", "115200", "--filter", "time" `
        -WorkingDirectory "C:\Users\praim\OneDrive\Documentos\PlatformIO\Projects\IA Kiln" `
        -NoNewWindow -PassThru -RedirectStandardOutput "nul" -RedirectStandardError "nul"
    
    Start-Sleep -Seconds 18
    
    # Intentar terminar el proceso
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
} catch {
    Write-Host "Error: $_"
}

Write-Host ""
Write-Host "Para ver los logs completos, abre el monitor serial manualmente:"
Write-Host "platformio device monitor -e esp32-s3-devkitc-1 --baud 115200"
Write-Host ""
Write-Host "Busca estas líneas en los logs cada 10 segundos:"
Write-Host "  [WiFi] ESTADO DE CONEXIÓN WiFi:"
Write-Host "  [WiFi] wifiConfigReceived: SÍ/NO"
Write-Host "  [WiFi] wifiConnected: SÍ/NO"
Write-Host "  [WiFi] IP: X.X.X.X"
Write-Host "  [Firebase] firebaseConnected: SÍ/NO"




















