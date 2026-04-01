@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

title 智慧农业监控系统

:: ==================== 自动提权为管理员（启动MySQL需要） ====================
net session >nul 2>&1
if !errorlevel! neq 0 (
    echo [提权] 需要管理员权限来启动 MySQL 服务，正在请求...
    powershell -NoProfile -Command "Start-Process cmd -ArgumentList '/c cd /d \"%~dp0\" && \"%~f0\"' -Verb RunAs"
    exit /b
)

echo.
echo ==================================================
echo   智慧农业监控系统 - 一键启动
echo ==================================================
echo   双击即可启动数据库 + 后端 + 前端，自动打开浏览器
echo ==================================================
echo.

:: ==================== 读取配置 ====================
set "CONFIG_FILE=%~dp0project.config.json"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=8081"
set "MYSQL_SVC="

if exist "%CONFIG_FILE%" (
    for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "(Get-Content '%CONFIG_FILE%' -Raw | ConvertFrom-Json).backend.port"`) do set "BACKEND_PORT=%%a"
    for /f "usebackq delims=" %%a in (`powershell -NoProfile -Command "(Get-Content '%CONFIG_FILE%' -Raw | ConvertFrom-Json).frontend.port"`) do set "FRONTEND_PORT=%%a"
)
echo   后端端口 = !BACKEND_PORT!
echo   前端端口 = !FRONTEND_PORT!
echo.

:: ==================== 1. 启动 MySQL ====================
echo [1/4] 启动 MySQL 数据库...

:: 自动检测 MySQL 服务名
for %%s in (MYSQL80 MYSQL MySQL57 MySQL56 MariaDB) do (
    sc query %%s >nul 2>&1
    if !errorlevel! equ 0 (
        set "MYSQL_SVC=%%s"
        goto :found_mysql
    )
)
echo   [X]  未找到 MySQL 服务！请确认已安装 MySQL。
echo        常见服务名: MYSQL80, MySQL, MySQL57
pause
exit /b 1

:found_mysql
:: 用 for /f 捕获结果，避免管道与延迟扩展的 errorlevel 冲突
set "MYSQL_RUNNING=0"
for /f "tokens=*" %%a in ('sc query !MYSQL_SVC! 2^>nul ^| findstr "RUNNING"') do set "MYSQL_RUNNING=1"
if "!MYSQL_RUNNING!"=="1" (
    echo   [OK] MySQL 已在运行 ^(!MYSQL_SVC!^)
) else (
    echo   --^>  正在启动 MySQL ^(!MYSQL_SVC!^)...
    net start !MYSQL_SVC! >nul 2>&1
    if !errorlevel! equ 0 (
        echo   [OK] MySQL 启动成功
    ) else (
        echo   [X]  MySQL 启动失败！请手动检查服务。
        pause
        exit /b 1
    )
)

:: ==================== 2. 检查环境 + 清理端口 ====================
echo [2/4] 检查环境...

set "BACKEND_PATH=%~dp0idea\agri-backend"
set "FRONTEND_PATH=%~dp0vue\agri-frontend"

if not exist "%BACKEND_PATH%\pom.xml" (
    echo   [X]  找不到后端项目: %BACKEND_PATH%\pom.xml
    pause
    exit /b 1
)
echo   [OK] 后端项目就绪

if not exist "%FRONTEND_PATH%\package.json" (
    echo   [X]  找不到前端项目: %FRONTEND_PATH%\package.json
    pause
    exit /b 1
)

if not exist "%FRONTEND_PATH%\node_modules" (
    echo   --^>  首次运行，安装前端依赖（约1-2分钟）...
    pushd "%FRONTEND_PATH%"
    call npm install
    popd
)
echo   [OK] 前端项目就绪

:: 清理被占用的端口
for %%P in (!BACKEND_PORT! !FRONTEND_PORT!) do (
    netstat -aon 2>nul | findstr ":%%P " | findstr "LISTENING" >nul 2>&1
    if !errorlevel! equ 0 (
        echo   --^>  端口 %%P 被占用，正在清理...
        for /f "tokens=5" %%i in ('netstat -aon 2^>nul ^| findstr ":%%P " ^| findstr "LISTENING"') do (
            taskkill /f /pid %%i >nul 2>&1
        )
        timeout /t 2 /nobreak >nul
    )
)
echo   [OK] 端口可用

:: ==================== 3. 启动后端 ====================
echo [3/4] 启动后端...

if exist "%BACKEND_PATH%\mvnw.cmd" (
    set "MVN=mvnw.cmd"
) else (
    set "MVN=mvn"
)

start "" /min cmd /k "title [后端] Spring Boot - 端口 !BACKEND_PORT! && color 0A && cd /d "%BACKEND_PATH%" && set SERVER_PORT=!BACKEND_PORT! && !MVN! spring-boot:run"

<nul set /p "=   等待后端启动"
set /a "W=0"
:wb
if !W! geq 20 (
    echo .
    echo   [X]  后端启动超时，请检查后端窗口日志
    goto :sf
)
netstat -aon 2>nul | findstr ":!BACKEND_PORT! " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo .
    echo   [OK] 后端启动成功  http://localhost:!BACKEND_PORT!
    goto :sf
)
<nul set /p "=."
set /a "W+=1"
timeout /t 3 /nobreak >nul
goto :wb

:: ==================== 4. 启动前端 ====================
:sf
echo [4/4] 启动前端...

start "" /min cmd /k "title [前端] Vue - 端口 !FRONTEND_PORT! && color 0B && cd /d "%FRONTEND_PATH%" && npm run serve"

<nul set /p "=   等待前端启动"
set /a "W=0"
:wf
if !W! geq 30 (
    echo .
    echo   [X]  前端启动超时，请检查前端窗口日志
    goto :done
)
netstat -aon 2>nul | findstr ":!FRONTEND_PORT! " | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo .
    echo   [OK] 前端启动成功  http://localhost:!FRONTEND_PORT!
    goto :done
)
<nul set /p "=."
set /a "W+=1"
timeout /t 3 /nobreak >nul
goto :wf

:: ==================== 完成，打开浏览器 ====================
:done
echo.
echo ==================================================
echo   全部启动完成！浏览器即将打开...
echo ==================================================
echo.
echo   前端  http://localhost:!FRONTEND_PORT!
echo   后端  http://localhost:!BACKEND_PORT!
echo.
echo   默认账号  admin / admin123
echo.
echo   关闭方式  关掉 [后端] [前端] 两个黑窗口即可
echo   端口修改  编辑 project.config.json
echo.

:: 等一下再开浏览器，确保前端完全就绪
timeout /t 3 /nobreak >nul
start "" "http://localhost:!FRONTEND_PORT!"

echo   浏览器已打开，本窗口可以关闭。
echo.
pause



