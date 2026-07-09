(ns ocvate.db
  "数据库连接池管理与 HugSQL 函数加载。"
  (:require [ocvate.config :as cfg]
            [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [hugsql.adapter.clojure-jdbc :as clj-adapter])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]
           [java.sql Connection]))

;; ──────────────────────────────────────────────────────────────────────
;; HikariCP 连接池（Oracle）
;; ──────────────────────────────────────────────────────────────────────

(defn make-datasource
  "根据 config.edn 的 :db 段创建 HikariCP 数据源。"
  [db-cfg]
  (let [hc (HikariConfig.)]
    (.setJdbcUrl      hc (str "jdbc:oracle:thin:@" (:host db-cfg) ":" (:port db-cfg) "/" (:dbname db-cfg)))
    (.setUsername     hc (:user db-cfg))
    (.setPassword     hc (:password db-cfg))
    (.setDriverClassName hc "oracle.jdbc.OracleDriver")
    (.setMaximumPoolSize    hc (or (:pool-size db-cfg) 10))
    (.setConnectionTimeout  hc (or (:connection-timeout-ms db-cfg) 30000))
    (.setIdleTimeout        hc (or (:idle-timeout-ms db-cfg) 600000))
    (.setMaxLifetime        hc (or (:max-lifetime-ms db-cfg) 1800000))
    (.setAutoCommit         hc false)    ;; Oracle 建议关闭自动提交
    (HikariDataSource. hc)))

(defonce ^:dynamic *datasource*
  (delay
    (let [db-cfg (cfg/db-config)]
      (println "🔌 连接数据库:" (:host db-cfg) ":" (:port db-cfg) "/" (:dbname db-cfg))
      (make-datasource db-cfg))))

(defn datasource
  "获取当前数据源。"
  []
  @*datasource*)

(defn get-connection
  "从连接池获取一个 JDBC 连接。"
  []
  (.getConnection ^HikariDataSource (datasource)))

(defn with-db [f]
  "在 JDBC 连接上下文中执行函数 f，f 接收一个连接参数。"
  (with-open [conn (get-connection)]
    (f conn)))

;; ──────────────────────────────────────────────────────────────────────
;; HugSQL — 将 sql/queries.sql 编译为 Clojure 函数
;; ──────────────────────────────────────────────────────────────────────

(hugsql/set-adapter! (clj-adapter/hugsql-adapter-clojure-jdbc))

;; 加载 SQL 文件，生成查询函数
;; 生成的函数名与 sql/queries.sql 中 -- :name 后面的标识符一致
;; 所有函数接受一个 map 参数（查询参数），最后一个参数是 datasource
;;
;; 例如 (get-assets-by-type {} (datasource))
;;
(hugsql/def-db-fns "queries.sql")

;; 也提供一个便捷宏版本（可选）
;; (hugsql/def-sqlvec-fns "sql/queries.sql")

(defn stop!
  "关闭连接池。"
  []
  (when (and (realized? *datasource*) @*datasource*)
    (println "🔌 关闭数据库连接池")
    (.close ^HikariDataSource @*datasource*)))
