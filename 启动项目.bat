@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo   智慧农业监控系统 - 启动脚本
echo ========================================
echo.

:: 读取配置文件
set "CONFIG_FILE=%~dp0project.config.json"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=8081"

if exist "%CONFIG_FILE%" (
    echo [配置] 正在读取 project.config.json...
    for /f "tokens=2 delims=:," %%a in ('findstr "port" "%CONFIG_FILE%"') do (
        set "port=%%~a"
        set "port=!port: =!"
        if "!BACKEND_PORT!"=="8080" (
            set "BACKEND_PORT=!port!"
        ) else (
            set "FRONTEND_PORT=!port!"
        )
    )
    echo [配置] 后端端口: !BACKEND_PORT!
    echo [配置] 前端端口: !FRONTEND_PORT!
) else (
    echo [配置] 未找到配置文件，使用默认端口
    echo [配置] 后端端口: !BACKEND_PORT!
    echo [配置] 前端端口: !FRONTEND_PORT!
)
echo.

echo [1/4] 检查MySQL服务状态...
sc query MYSQL80 | findstr "RUNNING" >nul
if %errorlevel% equ 0 (
    echo √ MySQL服务正在运行
) else (
    echo X MySQL服务未运行，请先启动MySQL服务！
    echo   执行命令: net start MYSQL80
    pause
    exit /b 1
)

echo [2/4] 检查端口占用情况...
netstat -an | findstr ":!BACKEND_PORT!" >nul
if %errorlevel% equ 0 (
    echo ! 端口 !BACKEND_PORT! 被占用，正在清理...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":!BACKEND_PORT!" ^| findstr "LISTENING"') do (
        taskkill /f /pid %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo √ 端口 !BACKEND_PORT! 已清理
) else (
    echo √ 端口 !BACKEND_PORT! 可用
)

netstat -an | findstr ":!FRONTEND_PORT!" >nul
if %errorlevel% equ 0 (
    echo ! 端口 !FRONTEND_PORT! 被占用，正在清理...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":!FRONTEND_PORT!" ^| findstr "LISTENING"') do (
        taskkill /f /pid %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo √ 端口 !FRONTEND_PORT! 已清理
) else (
    echo √ 端口 !FRONTEND_PORT! 可用
)

echo [3/4] 启动后端服务（端口 !BACKEND_PORT!）...
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "SERVER_PORT=!BACKEND_PORT!"
start "后端服务" cmd /k "cd /d %~dp0idea\agri-backend && set JAVA_HOME=C:\Program Files\Java\jdk-21 && set SERVER_PORT=!BACKEND_PORT! && mvn spring-boot:run"
echo √ 后端服务启动中...
echo   等待15秒让后端完全启动...
timeout /t 15 /nobreak >nul

echo [4/4] 启动前端服务（端口 !FRONTEND_PORT!）...
start "前端服务" cmd /k "cd /d %~dp0vue\agri-frontend && npm run serve"
echo √ 前端服务启动中...
echo   等待10秒让前端完全启动...
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo   启动完成！
echo ========================================
echo.
echo 访问地址：
echo   前端: http://localhost:!FRONTEND_PORT!
echo   后端: http://localhost:!BACKEND_PORT!
echo.
echo 默认账号：
echo   用户名: 梁景湖
echo   密码: 123456
echo.
echo 端口配置：
echo   修改 project.config.json 文件即可更改端口
echo.

:: 自动打开浏览器
start http://localhost:!FRONTEND_PORT!

pause



