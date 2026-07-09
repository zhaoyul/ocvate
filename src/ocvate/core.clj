(ns ocvate.core
  "企业资产图表分析 — 入口点。
   启动 Jetty Web 服务器，提供静态资源和 REST API。"
  (:require [ocvate.config :as cfg]
            [ocvate.db :as db]
            [ocvate.server :as server])
  (:import [org.eclipse.jetty.server Server])
  (:gen-class))

(defn -main
  "应用程序入口。"
  [& args]
  (println "╔══════════════════════════════════════════╗")
  (println "║     企业资产图表分析 — 服务端启动        ║")
  (println "╚══════════════════════════════════════════╝")
  (println)

  ;; 验证配置
  (let [db-cfg   (cfg/db-config)
        srv-cfg  (cfg/server-config)]
    (println "📋 服务器配置:")
    (println "  端口:" (:port srv-cfg 8080))
    (println "  静态根目录:" (:static-root srv-cfg "public"))
    (println)
    (println "📋 数据库配置:")
    (println "  URL: jdbc:oracle:thin:@" (:host db-cfg) ":" (:port db-cfg) "/" (:dbname db-cfg))
    (println "  用户名:" (:user db-cfg))
    (println "  连接池大小:" (:pool-size db-cfg 10))
    (println))

  ;; 初始化连接池（延迟初始化，在第一次请求时建立）
  (println "⏳ 数据库连接池就绪 (延迟连接)")
  (println)

  ;; 启动服务器
  (let [server (server/start)]
    ;; 注册关闭钩子
    (.addShutdownHook (Runtime/getRuntime)
      (Thread. (fn []
                 (println)
                 (println "⏳ 正在关闭...")
                 (server/stop server)
                 (db/stop!)
                 (println "✅ 已安全关闭"))))
    ;; 保持主线程运行
    (.join server)))
