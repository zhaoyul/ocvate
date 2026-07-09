@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"
set JAVA_CMD=runtime\bin\java.exe
set JAR=target\ocvate.jar

echo ╔══════════════════════════════════════════╗
echo ║   企业资产图表分析 — 一键启动           ║
echo ╚══════════════════════════════════════════╝
echo.

:: ── 1. 检查 JAR，不存在则构建 ──
if not exist "%JAR%" (
    echo [1/3] 构建 uber JAR...
    where clojure >nul 2>&1
    if errorlevel 1 (
        echo [错误] 需要 Clojure CLI 来构建
        echo 请运行: deploy\build.bat
        pause
        exit /b 1
    )
    clojure -M:uberdeps -m uberdeps.uberjar --main-class ocvate.core
    if errorlevel 1 (
        echo [错误] 构建失败
        pause
        exit /b 1
    )
) else (
    echo [1/3] JAR 已就绪
)

:: ── 2. 检查 Java 运行时，不存在则下载 ──
if not exist "%JAVA_CMD%" (
    echo [2/3] 下载 Java 运行时 (JDK 21 for Windows)...
    if not exist "runtime" mkdir runtime
    powershell -NoProfile -Command "
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;
        $url = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse';
        $zip = join-path $env:TEMP 'jdk21.zip';
        Write-Host '   下载中 (约 190MB)...';
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing;
        Write-Host '   解压中...';
        Expand-Archive -Path $zip -DestinationPath 'runtime_tmp' -Force;
        $src = Get-ChildItem 'runtime_tmp' -Directory | Select-Object -First 1;
        if ($src) { Copy-Item ($src.FullName + '\*') 'runtime\' -Recurse -Force };
        Remove-Item 'runtime_tmp' -Recurse -Force -ErrorAction SilentlyContinue;
        Remove-Item $zip -Force -ErrorAction SilentlyContinue;
    "
    if not exist "%JAVA_CMD%" (
        echo [错误] 下载失败，请手动下载解压到 runtime\
        echo   https://adoptium.net/temurin/releases/?version=21
        pause
        exit /b 1
    )
) else (
    echo [2/3] Java 运行时已就绪
    "%JAVA_CMD%" -version 2>&1 | findstr "version"
)

:: ── 3. 启动 ──
if not exist "data" mkdir data
echo [3/3] 启动服务...
echo.
echo   地址: http://127.0.0.1:8080
echo   停止: 关闭此窗口或 Ctrl+C
echo.

"%JAVA_CMD%" -Dconf=config-sqlite.edn -cp %JAR% clojure.main -m ocvate.core
pause
