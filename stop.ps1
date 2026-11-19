$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if (-not $conn) {
    Write-Host '后端未在 8080 端口运行'
    return
}

$procIds = $conn | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($procId in $procIds) {
    try {
        Stop-Process -Id $procId -Force
        Write-Host "已停止进程 $procId"
    } catch {
        Write-Host "停止进程 $procId 失败：$($_.Exception.Message)"
    }
}
