@echo off
chcp 65001 >nul
echo ========================================
echo   智慧农业监控系统 - 启动脚本
echo ========================================
echo.

echo [1/4] 检查MySQL服务状态...
sc query MYSQL80 | findstr "RUNNING" >nul
if %errorlevel% equ 0 (
    echo ✓ MySQL服务正在运行
) else (
    echo ✗ MySQL服务未运行，请先启动MySQL服务！
    echo   执行命令: net start MYSQL80
    pause
    exit /b 1
)

echo [2/4] 检查端口占用情况...
netstat -an | findstr ":8080" >nul
if %errorlevel% equ 0 (
    echo ⚠ 端口8080已被占用，后端可能已在运行
) else (
    echo ✓ 端口8080可用
)

netstat -an | findstr ":8081" >nul
if %errorlevel% equ 0 (
    echo ⚠ 端口8081已被占用，前端可能已在运行
) else (
    echo ✓ 端口8081可用
)

echo [3/4] 启动后端服务（端口8080）...
netstat -an | findstr ":8080" >nul
if not %errorlevel% equ 0 (
    echo 正在启动后端服务...
    start "后端服务" cmd /k "cd idea\agri-backend && mvn spring-boot:run"
    timeout /t 30 /nobreak >nul
    echo ✓ 后端服务启动中...
) else (
    echo ✓ 后端服务已在运行
)

echo [4/4] 启动前端服务（端口8081）...
netstat -an | findstr ":8081" >nul
if not %errorlevel% equ 0 (
    echo 正在启动前端服务...
    start "前端服务" cmd /k "cd vue\agri-frontend && npm run serve"
    timeout /t 20 /nobreak >nul
    echo ✓ 前端服务启动中...
) else (
    echo ✓ 前端服务已在运行
)

echo.
echo ========================================
echo   启动完成！
echo ========================================
echo.
echo 访问地址：
echo   前端: http://localhost:8080
echo   后端: 随机端口（请查看启动日志获取实际端口）
echo.
echo 默认账号：
echo   用户名: admin
echo   密码: admin123
echo.
echo 按任意键关闭此窗口...
pause >nul



