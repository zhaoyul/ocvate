@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"

echo ╔══════════════════════════════════════════╗
echo ║   企业资产图表分析 — 一键启动           ║
echo ╚══════════════════════════════════════════╝
echo.

:: 使用自带的 Java 运行时 (Windows JDK 21)
set JAVA_CMD=runtime\bin\java.exe
if not exist "%JAVA_CMD%" (
    echo [错误] 未找到运行时: %JAVA_CMD%
    pause
    exit /b 1
)

if not exist "target\ocvate.jar" (
    echo [错误] 未找到 target\ocvate.jar
    echo 请在有 Clojure CLI 的机器上运行 deploy\build.bat
    echo 然后将整个项目目录复制到本机
    pause
    exit /b 1
)

if not exist "data" mkdir data

echo 启动 SQLite 模式 (自动建表 + 测试数据)...
echo 地址: http://127.0.0.1:8080
echo 停止: 关闭此窗口
echo.

"%JAVA_CMD%" -Dconf=config-sqlite.edn -cp target\ocvate.jar clojure.main -m ocvate.core
pause
