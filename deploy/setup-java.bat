@echo off
chcp 65001 >nul
setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set RUNTIME_DIR=%PROJECT_DIR%\runtime
set JAVA_EXE=%RUNTIME_DIR%\bin\java.exe

echo ╔══════════════════════════════════════════╗
echo ║   企业资产图表分析 — 配置 Java 运行时    ║
echo ╚══════════════════════════════════════════╝
echo.

:: 如果已配置，跳过
if exist "%JAVA_EXE%" (
    echo [已配置] Java 运行时路径: %RUNTIME_DIR%
    "%JAVA_EXE%" -version 2>&1 | findstr "version"
    echo.
    echo 如需重置，删除 runtime 文件夹后重新运行
    pause
    exit /b 0
)

:: 方式1: 检测项目根目录下的 jdk 文件夹
if exist "%PROJECT_DIR%\jdk\bin\java.exe" (
    echo [复制] 从 %PROJECT_DIR%\jdk\ 复制到 runtime\...
    robocopy "%PROJECT_DIR%\jdk" "%RUNTIME_DIR%" /E /NP /NFL /NDL >nul
    if exist "%JAVA_EXE%" (
        echo ✅ 配置完成
        pause
        exit /b 0
    )
)

:: 方式2: 检测当前目录旁边的 jdk 文件夹
if exist "%~dp0jdk\bin\java.exe" (
    echo [复制] 从 deploy\jdk\ 复制到 runtime\...
    robocopy "%~dp0jdk" "%RUNTIME_DIR%" /E /NP /NFL /NDL >nul
    if exist "%JAVA_EXE%" (
        echo ✅ 配置完成
        pause
        exit /b 0
    )
)

echo [错误] 未找到 Java 运行时
echo.
echo 请将解压后的 JDK 21 文件夹重命名为 jdk，放在项目根目录，
echo 或直接放在 runtime\ 目录下。
echo.
echo 下载地址: https://adoptium.net/temurin/releases/?version=21
echo 选择: Windows x64, JDK 21 (LTS), .zip 格式
echo.
pause
