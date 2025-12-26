# 智慧农业监控系统 - 启动脚本
# 使用方法：在PowerShell中执行 .\启动项目.ps1

Write-Host "========================================" -ForegroundColor Green
Write-Host "  智慧农业监控系统 - 启动脚本" -ForegroundColor Green
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

# 检查端口占用
Write-Host "[2/4] 检查端口占用情况..." -ForegroundColor Yellow
$port8080 = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
$port8081 = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue

if ($port8080) {
    Write-Host "⚠ 端口8080已被占用，后端可能已在运行" -ForegroundColor Yellow
} else {
    Write-Host "✓ 端口8080可用" -ForegroundColor Green
}

if ($port8081) {
    Write-Host "⚠ 端口8081已被占用，前端可能已在运行" -ForegroundColor Yellow
} else {
    Write-Host "✓ 端口8081可用" -ForegroundColor Green
}

# 启动后端
Write-Host "[3/4] 启动后端服务（端口8080）..." -ForegroundColor Yellow
if (-not $port8080) {
    $backendJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        cd idea/agri-backend
        mvn spring-boot:run
    }
    Write-Host "✓ 后端服务启动中..." -ForegroundColor Green
    Write-Host "  等待30秒让后端完全启动..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
} else {
    Write-Host "✓ 后端服务已在运行" -ForegroundColor Green
}

# 启动前端
Write-Host "[4/4] 启动前端服务（端口8081）..." -ForegroundColor Yellow
if (-not $port8081) {
    $frontendJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        cd vue/agri-frontend
        npm run serve
    }
    Write-Host "✓ 前端服务启动中..." -ForegroundColor Green
    Write-Host "  等待20秒让前端完全启动..." -ForegroundColor Yellow
    Start-Sleep -Seconds 20
} else {
    Write-Host "✓ 前端服务已在运行" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  启动完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "访问地址：" -ForegroundColor Cyan
Write-Host "  前端: http://localhost:8080" -ForegroundColor White
Write-Host "  后端: 随机端口（请查看启动日志获取实际端口）" -ForegroundColor White
Write-Host ""
Write-Host "默认账号：" -ForegroundColor Cyan
Write-Host "  用户名: admin" -ForegroundColor White
Write-Host "  密码: admin123" -ForegroundColor White
Write-Host ""
Write-Host "查看日志：" -ForegroundColor Cyan
Write-Host "  后端: Receive-Job -Id $backendJob.Id" -ForegroundColor White
Write-Host "  前端: Receive-Job -Id $frontendJob.Id" -ForegroundColor White
Write-Host ""
Write-Host "停止服务：" -ForegroundColor Cyan
Write-Host "  Stop-Job -Id $backendJob.Id, $frontendJob.Id" -ForegroundColor White
Write-Host ""



