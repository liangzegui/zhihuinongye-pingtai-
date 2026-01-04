# 智慧农业监控系统 - 启动脚本
# 使用方法：在PowerShell中执行 .\启动项目.ps1
# 端口配置：修改 project.config.json 即可自动生效

$ErrorActionPreference = "SilentlyContinue"
$projectRoot = $PSScriptRoot

# 读取配置文件
$configPath = Join-Path $projectRoot "project.config.json"
if (Test-Path $configPath) {
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    $backendPort = $config.backend.port
    $frontendPort = $config.frontend.port
    Write-Host "[配置] 已加载 project.config.json" -ForegroundColor Green
} else {
    $backendPort = 8080
    $frontendPort = 8081
    Write-Host "[配置] 未找到配置文件，使用默认端口" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  智慧农业监控系统 - 启动脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "  后端端口: $backendPort" -ForegroundColor Cyan
Write-Host "  前端端口: $frontendPort" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# 检查MySQL服务
Write-Host "[1/4] 检查MySQL服务状态..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "*mysql*" -ErrorAction SilentlyContinue | Where-Object {$_.Status -eq "Running"}
if ($mysqlService) {
    Write-Host "✓ MySQL服务正在运行" -ForegroundColor Green
} else {
    Write-Host "✗ MySQL服务未运行，请先启动MySQL服务！" -ForegroundColor Red
    Write-Host "  执行命令: Start-Service -Name MYSQL80" -ForegroundColor Yellow
    exit 1
}

# 检查并清理端口占用
Write-Host "[2/4] 检查端口占用情况..." -ForegroundColor Yellow

function Clear-Port($port) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        Write-Host "⚠ 端口 $port 被占用，正在清理..." -ForegroundColor Yellow
        $conn | ForEach-Object {
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 2
        Write-Host "✓ 端口 $port 已清理" -ForegroundColor Green
        return $false
    } else {
        Write-Host "✓ 端口 $port 可用" -ForegroundColor Green
        return $true
    }
}

Clear-Port $backendPort | Out-Null
Clear-Port $frontendPort | Out-Null

# 启动后端
Write-Host "[3/4] 启动后端服务（端口 $backendPort）..." -ForegroundColor Yellow
$backendPath = Join-Path $projectRoot "idea\agri-backend"

# 设置环境变量并启动后端
$backendCmd = "cd /d `"$backendPath`" && set JAVA_HOME=C:\Program Files\Java\jdk-21 && set SERVER_PORT=$backendPort && mvn spring-boot:run"
Start-Process -FilePath "cmd" -ArgumentList "/k", $backendCmd -WindowStyle Normal

Write-Host "✓ 后端服务启动中..." -ForegroundColor Green
Write-Host "  等待15秒让后端完全启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# 启动前端
Write-Host "[4/4] 启动前端服务（端口 $frontendPort）..." -ForegroundColor Yellow
$frontendPath = Join-Path $projectRoot "vue\agri-frontend"

Start-Process -FilePath "cmd" -ArgumentList "/k", "cd /d `"$frontendPath`" && npm run serve" -WindowStyle Normal

Write-Host "✓ 前端服务启动中..." -ForegroundColor Green
Write-Host "  等待10秒让前端完全启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  启动完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "访问地址：" -ForegroundColor Cyan
Write-Host "  前端: http://localhost:$frontendPort" -ForegroundColor White
Write-Host "  后端: http://localhost:$backendPort" -ForegroundColor White
Write-Host ""
Write-Host "默认账号：" -ForegroundColor Cyan
Write-Host "  用户名: 梁景湖" -ForegroundColor White
Write-Host "  密码: 123456" -ForegroundColor White
Write-Host ""
Write-Host "端口配置：" -ForegroundColor Cyan
Write-Host "  修改 project.config.json 文件即可更改端口" -ForegroundColor Gray
Write-Host ""

# 自动打开浏览器
Start-Process "http://localhost:$frontendPort"



