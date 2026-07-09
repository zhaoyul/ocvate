(ns ocvate.server
  "Web 服务器 — 路由与处理器。"
  (:require [ocvate.config :as cfg]
            [ocvate.db :as db]
            [compojure.core :refer [defroutes GET POST context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :as resp]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]))

;; ──────────────────────────────────────────────────────────────────────
;; API 处理器
;; ──────────────────────────────────────────────────────────────────────

(defn- json-response
  "返回 JSON 响应，设置 UTF-8 编码与 CORS 头。"
  [data]
  (-> (resp/response (json/generate-string data))
      (resp/header "Content-Type" "application/json; charset=utf-8")
      (resp/header "Access-Control-Allow-Origin" "*")))

(defn- api-query
  "通用 API: 执行 HugSQL 查询函数并返回 JSON。
   query-fn 是从 sql/queries.sql 编译出来的函数。"
  [query-fn params]
  (try
    (let [result (query-fn (merge {} params) (db/datasource))]
      (json-response {:ok true :data result}))
    (catch Exception e
      (json-response {:ok false :error (.getMessage e)}))))

;; ─── 暴露的 API 路由 ───

(defn api-assets-by-type [_]
  (api-query db/get-assets-by-type {}))

(defn api-assets-by-dept [_]
  (api-query db/get-assets-by-dept {}))

(defn api-depreciation [_]
  (api-query db/get-depreciation-summary {}))

(defn api-annual-dynamics [_]
  (api-query db/get-annual-dynamics {}))

(defn api-outbound [_]
  (api-query db/get-outbound-summary {}))

(defn api-repair [_]
  (api-query db/get-repair-summary {}))

(defn api-fuel [_]
  (api-query db/get-fuel-summary {}))

(defn api-assets-by-year [req]
  (let [year (get-in req [:params :year])]
    (api-query db/get-assets-by-year {:fiscal_year year})))



;; ──────────────────────────────────────────────────────────────────────
;; 健康检查
;; ──────────────────────────────────────────────────────────────────────

(defn api-health [_]
  (try
    (with-open [conn (db/get-connection)]
      (let [rs (.createStatement conn)
            ok (.execute rs "SELECT 1 FROM dual")]
        (json-response {:ok true :database "connected" :status "healthy"})))
    (catch Exception e
      (json-response {:ok false :database "disconnected" :error (.getMessage e)}))))

;; ──────────────────────────────────────────────────────────────────────
;; 静态文件服务
;; ──────────────────────────────────────────────────────────────────────

(defn serve-index
  "提供 index.html。"
  [_]
  (let [idx (io/resource "public/index.html")]
    (if idx
      (-> (resp/resource-response "public/index.html")
          (resp/content-type "text/html; charset=utf-8"))
      (resp/not-found "<h1>404 — index.html 未找到</h1>"))))

;; ──────────────────────────────────────────────────────────────────────
;; 路由定义
;; ──────────────────────────────────────────────────────────────────────

(defroutes app-routes
  ;; API 端点
  (GET  "/api/health"             [] api-health)
  (GET  "/api/assets/by-type"     [] api-assets-by-type)
  (GET  "/api/assets/by-dept"     [] api-assets-by-dept)
  (GET  "/api/assets/by-year"     [year] api-assets-by-year)
  (GET  "/api/depreciation"       [] api-depreciation)
  (GET  "/api/annual-dynamics"    [] api-annual-dynamics)
  (GET  "/api/outbound"           [] api-outbound)
  (GET  "/api/repair"             [] api-repair)
  (GET  "/api/fuel"               [] api-fuel)


  ;; 静态资源（js/css/图片等）
  (route/resources "/" {:root "public"})

  ;; 兜底: 所有其他路由返回 index.html（SPA 支持）
  (GET "/*" [] serve-index))

;; ──────────────────────────────────────────────────────────────────────
;; 中间件栈
;; ──────────────────────────────────────────────────────────────────────

(def app
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-not-modified))

;; ──────────────────────────────────────────────────────────────────────
;; 启动 / 停止
;; ──────────────────────────────────────────────────────────────────────

(defn start
  "启动 Jetty 服务器。返回 Server 实例。"
  []
  (let [port (get (cfg/server-config) :port 8080)]
    (let [server (jetty/run-jetty app {:port port :join? false})]
      (println (str "🌐 服务器启动: http://127.0.0.1:" port))
      (println (str "📄 健康检查: http://127.0.0.1:" port "/api/health"))
      server)))

(defn stop
  "停止 Jetty 服务器。"
  [server]
  (when server
    (.stop ^org.eclipse.jetty.server.Server server)
    (println "🌐 服务器已停止")))
