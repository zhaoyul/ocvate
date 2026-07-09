#!/bin/bash
cd "$(dirname "$0")"

echo "╔══════════════════════════════════════════╗"
echo "║   企业资产图表分析 — 一键启动           ║"
echo "╚══════════════════════════════════════════╝"
echo

# macOS/Linux: 不使用 Windows runtime, 用系统 Java
JAVA_CMD=$(which java 2>/dev/null)
if [ -z "$JAVA_CMD" ]; then
    echo "[错误] 未找到 Java，请安装 JDK 17+"
    exit 1
fi

if [ ! -f "target/ocvate.jar" ]; then
    echo "[错误] 未找到 target/ocvate.jar"
    echo "请运行: clojure -M:uberdeps -m uberdeps.uberjar --main-class ocvate.core"
    exit 1
fi

mkdir -p data
echo "启动 SQLite 模式..."
echo "地址: http://127.0.0.1:8080"
echo "停止: Ctrl+C"
echo

exec "$JAVA_CMD" -Dconf=config-sqlite.edn -cp target/ocvate.jar clojure.main -m ocvate.core
