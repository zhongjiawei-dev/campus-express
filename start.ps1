param(
    [ValidateSet('dev', 'mysql')]
    [string]$Profile = 'dev'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    $javaHome = 'C:\Program Files\Java\jdk-17'
}
if (-not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    $jdk = Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $javaHome = $jdk.FullName }
}
if (-not (Test-Path (Join-Path $javaHome 'bin\javac.exe'))) {
    throw "未找到 JDK 17，请安装 JDK 17 并设置 JAVA_HOME"
}
$env:JAVA_HOME = $javaHome

$jar = Join-Path $root 'target\campus-express-1.0.0.jar'
$mvn = Join-Path $root 'tools\apache-maven-3.9.9\bin\mvn.cmd'

if (-not (Test-Path $mvn)) {
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        $mvn = $mvnCmd.Source
    } else {
        throw "未找到 Maven：请把 Maven 3.9 解压到 tools\apache-maven-3.9.9，或把 mvn 加入 PATH"
    }
}

Write-Host '正在编译打包（首次约 1-2 分钟，之后约 3-10 秒）...'
& $mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { throw '打包失败，请查看上方 Maven 输出' }

if (-not (Test-Path (Join-Path $root 'logs'))) {
    New-Item -ItemType Directory (Join-Path $root 'logs') | Out-Null
}

$javaArgs = @('-jar', $jar)
if ($Profile -eq 'mysql') {
    $javaArgs += '--spring.profiles.active=mysql'
    Write-Host '使用 MySQL 配置启动（需先执行 sql/schema-mysql.sql 并创建 campus 账号）'
} else {
    Write-Host '使用 H2 内存库启动（重启后数据重置）'
}

Write-Host '服务地址：http://localhost:8080/api/v1'
Write-Host '管理后台：admin/index.html'
Write-Host '按 Ctrl+C 停止服务'
& (Join-Path $javaHome 'bin\java.exe') @javaArgs
