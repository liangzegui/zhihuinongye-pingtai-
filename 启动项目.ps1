# 智慧农业监控系统 - PowerShell 一键启动脚本
# 双击运行或在终端执行: .\启动项目.ps1
# 停止所有服务: .\启动项目.ps1 -Stop
# 自动完成: 启动MySQL → 启动后端 → 启动前端 → 打开浏览器

param(
    [switch]$Stop
)

$ErrorActionPreference = "Stop"
$projectRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }

# ==================== 自动提权（启动MySQL需要管理员） ====================

$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "[提权] 需要管理员权限来启动 MySQL 服务，正在请求..." -ForegroundColor Yellow
    $argList = "-NoProfile -ExecutionPolicy Bypass -File `"$($MyInvocation.MyCommand.Path)`""
    if ($Stop) { $argList += " -Stop" }
    Start-Process powershell -ArgumentList $argList -Verb RunAs -WorkingDirectory $projectRoot
    exit
}

# ==================== 工具函数 ====================

function Write-Banner {
    param([string]$Text, [string]$Color = "Green")
    Write-Host ""
    Write-Host ("=" * 50) -ForegroundColor $Color
    Write-Host "  $Text" -ForegroundColor $Color
    Write-Host ("=" * 50) -ForegroundColor $Color
    Write-Host ""
}

function Write-Step {
    param([string]$Step, [string]$Message)
    Write-Host "[$Step] $Message" -ForegroundColor Yellow
}

function Write-Ok {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "  [X]  $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  ->   $Message" -ForegroundColor Gray
}

function Test-PortInUse {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
            Where-Object { $_.State -eq "Listen" }
    return ($null -ne $conn)
}

function Stop-PortProcess {
    param([int]$Port)
    $conns = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
             Where-Object { $_.State -eq "Listen" }
    if ($conns) {
        $conns | ForEach-Object {
            $proc = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
            if ($proc) {
                Write-Info "终止进程: $($proc.ProcessName) (PID: $($proc.Id))"
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            }
        }
        Start-Sleep -Seconds 2
    }
}

function Wait-ForPort {
    param([int]$Port, [int]$TimeoutSeconds = 60)
    $elapsed = 0
    $interval = 3
    while ($elapsed -lt $TimeoutSeconds) {
        if (Test-PortInUse -Port $Port) {
            Write-Host "" # 换行
            return $true
        }
        Write-Host "." -NoNewline -ForegroundColor DarkGray
        Start-Sleep -Seconds $interval
        $elapsed += $interval
    }
    Write-Host ""
    return $false
}

# ==================== 读取配置 ====================

$configPath = Join-Path $projectRoot "project.config.json"
$backendPort = 8080
$frontendPort = 8081

if (Test-Path $configPath) {
    try {
        $config = Get-Content $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $backendPort = $config.backend.port
        $frontendPort = $config.frontend.port
    } catch {
        Write-Host "[配置] 解析配置文件失败，使用默认端口" -ForegroundColor Yellow
    }
} else {
    Write-Host "[配置] 未找到 project.config.json，使用默认端口" -ForegroundColor Yellow
}

# ==================== 停止模式 ====================

if ($Stop) {
    Write-Banner "智慧农业监控系统 - 停止服务" "Red"

    Write-Step "1/3" "停止后端服务 (端口 $backendPort)..."
    if (Test-PortInUse -Port $backendPort) {
        Stop-PortProcess -Port $backendPort
        Write-Ok "后端服务已停止"
    } else {
        Write-Info "后端服务未运行"
    }

    Write-Step "2/3" "停止前端服务 (端口 $frontendPort)..."
    if (Test-PortInUse -Port $frontendPort) {
        Stop-PortProcess -Port $frontendPort
        Write-Ok "前端服务已停止"
    } else {
        Write-Info "前端服务未运行"
    }

    # 清理残留 node 进程
    Get-Process -Name "node" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

    Write-Step "3/3" "停止 MySQL 服务..."
    $mysqlSvc = Get-Service -Name "*mysql*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($mysqlSvc -and $mysqlSvc.Status -eq "Running") {
        Stop-Service -Name $mysqlSvc.Name -Force -ErrorAction SilentlyContinue
        Write-Ok "MySQL 已停止"
    } else {
        Write-Info "MySQL 未运行"
    }

    Write-Banner "所有服务已停止" "Red"
    exit 0
}

# ==================== 启动模式 ====================

Write-Banner "智慧农业监控系统 - 启动脚本"
Write-Host "  后端端口: $backendPort" -ForegroundColor Cyan
Write-Host "  前端端口: $frontendPort" -ForegroundColor Cyan
Write-Host ""

# ---------- 步骤 1: 启动 MySQL ----------
Write-Step "1/5" "启动 MySQL 数据库..."

$mysqlSvc = Get-Service -Name "*mysql*" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $mysqlSvc) {
    Write-Fail "未找到 MySQL 服务！请确认已安装 MySQL。"
    Read-Host "按回车键退出"
    exit 1
}

if ($mysqlSvc.Status -eq "Running") {
    Write-Ok "MySQL 已在运行 ($($mysqlSvc.Name))"
} else {
    Write-Info "正在启动 MySQL ($($mysqlSvc.Name))..."
    try {
        Start-Service -Name $mysqlSvc.Name -ErrorAction Stop
        Start-Sleep -Seconds 3
        Write-Ok "MySQL 启动成功"
    } catch {
        Write-Fail "MySQL 启动失败: $_"
        Read-Host "按回车键退出"
        exit 1
    }
}

# ---------- 步骤 2: 检查环境 ----------
Write-Step "2/5" "检查运行环境..."

$backendPath = Join-Path $projectRoot "idea\agri-backend"
$frontendPath = Join-Path $projectRoot "vue\agri-frontend"

if (-not (Test-Path (Join-Path $backendPath "pom.xml"))) {
    Write-Fail "找不到后端项目: $backendPath\pom.xml"
    exit 1
}
Write-Ok "后端项目目录正常"

if (-not (Test-Path (Join-Path $frontendPath "package.json"))) {
    Write-Fail "找不到前端项目: $frontendPath\package.json"
    exit 1
}

if (-not (Test-Path (Join-Path $frontendPath "node_modules"))) {
    Write-Info "前端依赖未安装，正在执行 npm install..."
    Push-Location $frontendPath
    & npm install 2>&1 | Out-Null
    Pop-Location
}
Write-Ok "前端项目目录正常"

# ---------- 步骤 3: 清理端口 ----------
Write-Step "3/5" "检查端口占用..."

if (Test-PortInUse -Port $backendPort) {
    Write-Info "端口 $backendPort 被占用，正在清理..."
    Stop-PortProcess -Port $backendPort
    Write-Ok "端口 $backendPort 已释放"
} else {
    Write-Ok "端口 $backendPort 可用"
}

if (Test-PortInUse -Port $frontendPort) {
    Write-Info "端口 $frontendPort 被占用，正在清理..."
    Stop-PortProcess -Port $frontendPort
    Write-Ok "端口 $frontendPort 已释放"
} else {
    Write-Ok "端口 $frontendPort 可用"
}

# ---------- 步骤 4: 启动后端 ----------
Write-Step "4/5" "启动后端服务 (端口 $backendPort)..."

$mvnwPath = Join-Path $backendPath "mvnw.cmd"
if (Test-Path $mvnwPath) {
    $buildTool = ".\mvnw.cmd"
} else {
    $buildTool = "mvn"
}
$backendCmd = "cd /d `"$backendPath`" && set SERVER_PORT=$backendPort && $buildTool spring-boot:run"
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "title [后端] Spring Boot && $backendCmd" -WindowStyle Normal

Write-Info "等待后端启动 (最多60秒)..."
$backendReady = Wait-ForPort -Port $backendPort -TimeoutSeconds 60

if ($backendReady) {
    Start-Sleep -Seconds 3
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:$backendPort/auth/login" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
        Write-Ok "后端服务启动成功 (HTTP 可达)"
    } catch {
        Write-Ok "后端服务已启动 (端口 $backendPort 已监听)"
    }
} else {
    Write-Fail "后端服务启动超时！请检查后端窗口的日志"
    Write-Info "继续尝试启动前端..."
}

# ---------- 步骤 5: 启动前端 ----------
Write-Step "5/5" "启动前端服务 (端口 $frontendPort)..."

$frontendCmd = "cd /d `"$frontendPath`" && npm run serve"
Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "title [前端] Vue && $frontendCmd" -WindowStyle Normal

Write-Info "等待前端启动 (最多90秒)..."
$frontendReady = Wait-ForPort -Port $frontendPort -TimeoutSeconds 90

if ($frontendReady) {
    Write-Ok "前端服务启动成功"
} else {
    Write-Fail "前端服务启动超时！请检查前端窗口的日志"
}

# ==================== 启动完成 ====================

Write-Banner "启动完成！"

$fStatus = if ($frontendReady) { "[OK]" } else { "[X] " }
$bStatus = if ($backendReady)  { "[OK]" } else { "[X] " }

Write-Host "  服务状态:" -ForegroundColor Cyan
Write-Host "    $bStatus 后端  http://localhost:$backendPort" -ForegroundColor $(if ($backendReady) { "Green" } else { "Red" })
Write-Host "    $fStatus 前端  http://localhost:$frontendPort" -ForegroundColor $(if ($frontendReady) { "Green" } else { "Red" })
Write-Host ""
Write-Host "  默认账号:" -ForegroundColor Cyan
Write-Host "    用户名  admin" -ForegroundColor White
Write-Host "    密码    admin123" -ForegroundColor White
Write-Host ""
Write-Host "  常用操作:" -ForegroundColor Cyan
Write-Host "    停止服务      .\启动项目.ps1 -Stop" -ForegroundColor Gray
Write-Host "    修改端口      编辑 project.config.json" -ForegroundColor Gray
Write-Host "    关闭方式      关掉 [后端] [前端] 两个窗口即可" -ForegroundColor Gray
Write-Host ""

# 自动打开浏览器
if ($frontendReady) {
    Write-Host "  浏览器即将打开..." -ForegroundColor Cyan
    Start-Sleep -Seconds 3
    Start-Process "http://localhost:$frontendPort"
}

Read-Host "按回车键退出此窗口（服务会继续在后台运行）"



