(ns ocvate.server
  "Web 服务器 — 路由与处理器。"
  (:require [ocvate.config :as cfg]
            [ocvate.db :as db]
            [compojure.core :refer [defroutes GET context]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :as resp]
            [cheshire.core :as json]
            [cheshire.generate :as json-gen]
            [ring.adapter.jetty :as jetty]))

;; ──────────────────────────────────────────────────────────────────────
;; Key 转换: SQLite 返回小写下划线 key → 前端 camelCase
;; ──────────────────────────────────────────────────────────────────────

(defn- snake->camel
  "将 snake_case 关键字转为 camelCase 字符串。用来处理 SQLite/Oracle 返回的列名。\n   Oracle 可能返回大写下划线 key，先统一转小写再转换。"
  [s]
  (if (keyword? s)
    (let [n (-> s name clojure.string/lower-case)]
      (->> (clojure.string/split n #"_")
           (map-indexed (fn [i part] (if (zero? i) part (clojure.string/capitalize part))))
           (clojure.string/join)))
    (name s)))

(defn- transform-map
  "递归转换 map 的 key 为 camelCase。"
  [m]
  (into {} (map (fn [[k v]]
                  [(snake->camel k)
                   (if (map? v) (transform-map v) v)])
                m)))

(defn- transform-rows
  "转换查询结果行。"
  [rows]
  (mapv transform-map rows))

;; ──────────────────────────────────────────────────────────────────────
;; API 处理器
;; ──────────────────────────────────────────────────────────────────────

(defn- json-response
  "返回 JSON 响应。"
  [data]
  (-> (resp/response (json/generate-string data))
      (resp/header "Content-Type" "application/json; charset=utf-8")
      (resp/header "Access-Control-Allow-Origin" "*")))

(defn- ok [data]
  (json-response {:ok true :data data}))

(defn- err [msg]
  (json-response {:ok false :error msg}))

(def ^:private default-session-ttl-ms (* 3 60 60 1000))
(defonce ^:private sessions (atom {}))

(defn- issue-session! []
  (let [now (System/currentTimeMillis)
        session-ttl-ms (get (cfg/auth-config) :session-ttl-ms default-session-ttl-ms)
        token (str (java.util.UUID/randomUUID))
        expires-at (+ now session-ttl-ms)]
    (swap! sessions (fn [active]
                      (assoc (into {}
                                   (remove (fn [[_ expiry]] (<= expiry now)) active))
                             token
                             expires-at)))
    {:token token :expires-at expires-at :ttl-ms session-ttl-ms}))

(defn- authorized? [req]
  (let [token (get-in req [:cookies "ocvate_access" :value])
        expires-at (get @sessions token)]
    (and expires-at
         (> expires-at (System/currentTimeMillis)))))

(defn- expired-session-response [req]
  (if (clojure.string/starts-with? (or (:uri req) "") "/api/")
    (-> (json-response {:ok false
                        :error "SESSION_EXPIRED"
                        :message "已离线，请重新登录或重新创建连接。"})
        (resp/status 401))
    (-> (resp/response "<!doctype html><html lang=\"zh-CN\"><meta charset=\"utf-8\"><title>连接已离线</title><style>html,body{margin:0;min-height:100%;background:#07101d;color:#eef2ff;font-family:Microsoft YaHei,Segoe UI,sans-serif}body{min-height:100vh;display:grid;place-items:center;overflow:hidden}body:before{content:\"\";position:fixed;inset:0;opacity:.28;background-image:linear-gradient(rgba(144,205,244,.035) 1px,transparent 1px),linear-gradient(90deg,rgba(144,205,244,.035) 1px,transparent 1px);background-size:48px 48px;pointer-events:none}.offline{text-align:center;position:relative;z-index:1}.offline-mark{width:54px;height:54px;margin:0 auto 22px;border:1px solid rgba(144,205,244,.32);border-radius:16px;display:grid;place-items:center;color:#90cdf4;font-size:25px;background:rgba(15,29,50,.72);box-shadow:0 0 30px rgba(59,130,246,.12)}h1{font-size:24px;letter-spacing:2px;margin:0 0 14px}p{margin:0;color:#9eb4da;font-size:14px}</style><body><main class=\"offline\"><div class=\"offline-mark\">⌁</div><h1>已离线</h1><p>连接已过期，请重新登录或重新创建连接。</p></main></body></html>")
        (resp/status 401)
        (resp/content-type "text/html; charset=utf-8"))))

(defn- access-gate
  "按配置要求固定 key；会话为随机 token，绝对有效期 3 小时。"
  [handler]
  (fn [req]
    (let [uri (or (:uri req) "/")
          supplied-key (get-in req [:params :key])
          query-without-key (->> (clojure.string/split (or (:query-string req) "") #"&")
                                 (remove #(clojure.string/starts-with? % "key="))
                                 (clojure.string/join "&"))
          redirect-uri (if (seq query-without-key)
                         (str uri "?" query-without-key)
                         uri)
          auth-config (cfg/auth-config)
          auth-enabled? (not= false (:enabled auth-config))
          access-key (or (:access-key auth-config) "")
          health? (= uri "/api/health")
        page-entry? (and (= supplied-key access-key)
                           (not (clojure.string/starts-with? uri "/api/")))]
      (cond
        ;; 无论当前是否已有会话，只要页面 URL 携带 key，就先清理地址栏中的敏感参数。
        (and page-entry? (authorized? req))
        (resp/redirect redirect-uri)
        (not auth-enabled?) (handler req)
        (authorized? req) (handler req)
        health? (handler req)
        page-entry? (let [{:keys [token expires-at ttl-ms]} (issue-session!)]
                      (-> (resp/redirect redirect-uri)
                          (resp/set-cookie "ocvate_access" token
                                            {:http-only true :same-site :lax
                                             :max-age (long (/ ttl-ms 1000))})
                          ;; 只暴露过期时间，不暴露会话 token，供前端锁屏计时使用。
                          (resp/set-cookie "ocvate_session_expires_at" (str expires-at)
                                            {:http-only false :same-site :lax
                                             :max-age (long (/ ttl-ms 1000))})))
        :else (expired-session-response req)))))

;; ─── 健康检查 ───

(defn api-health [_]
  (try
    (db/query "SELECT 1 AS ok")
    (json-response {:ok true :database "connected" :status "healthy"})
    (catch Exception e
      (json-response {:ok false :database "disconnected" :error (.getMessage e)}))))

;; ─── 资产相关 ───

(defn api-assets-by-type [_]
  (try (ok (transform-rows (db/get-assets-by-type)))
       (catch Exception e (err (.getMessage e)))))

(defn api-assets-by-dept [_]
  (try (ok (transform-rows (db/get-assets-by-dept)))
       (catch Exception e (err (.getMessage e)))))

(defn api-depreciation [_]
  (try (ok (transform-rows (db/get-depreciation-summary)))
       (catch Exception e (err (.getMessage e)))))

(defn api-annual-dynamics [_]
  (try (ok (transform-rows (db/get-annual-dynamics)))
       (catch Exception e (err (.getMessage e)))))

;; ─── 出库 ───

(defn api-outbound-summary [_]
  (try (ok (transform-rows (db/get-outbound-summary)))
       (catch Exception e (err (.getMessage e)))))

(defn api-outbound-details [_]
  (try (ok (transform-rows (db/get-outbound-details)))
       (catch Exception e (err (.getMessage e)))))

;; ─── 维修 ───

(defn api-repairs [_]
  (try (ok (transform-rows (db/get-repairs)))
       (catch Exception e (err (.getMessage e)))))

(defn api-repairs-single-device [_]
  (try (ok (transform-rows (db/get-repairs-single-device)))
       (catch Exception e (err (.getMessage e)))))

(defn api-repairs-department [_]
  (try (ok (transform-rows (db/get-repairs-department)))
       (catch Exception e (err (.getMessage e)))))

;; ─── 油耗 ───

(defn api-monthly-fuel [_]
  (try (ok (transform-rows (db/get-monthly-fuel)))
       (catch Exception e (err (.getMessage e)))))

(defn api-department-fuel [_]
  (try (ok (transform-rows (db/get-department-fuel)))
       (catch Exception e (err (.getMessage e)))))

(defn api-department-energy [_]
  (try (ok (transform-rows (db/get-department-energy)))
       (catch Exception e (err (.getMessage e)))))

(defn api-vehicle-fuel [_]
  (try (ok (transform-rows (db/get-vehicle-fuel)))
       (catch Exception e (err (.getMessage e)))))

;; ─── 聚合 API：一次返回前端所需全部数据 ───

(def ^:private api-key->view
  "聚合 API 返回 key 到实际视图名的映射（从配置读取，Oracle 可覆盖）。"
  (into {} (map (fn [k] [k (cfg/view-name :oracle k)])) (keys (cfg/default-view-names))))

(defn api-all [_]
  (try
    (println "📡 前端请求 /api/all")
    (let [queries {:departments        db/get-assets-by-dept
                  :assetTypes         db/get-assets-by-type
                  :depreciation       db/get-depreciation-summary
                  :annualDynamics     db/get-annual-dynamics
                  :outboundSummary    db/get-outbound-summary
                  :outboundDetails    db/get-outbound-details
                  :repairs            db/get-repairs
                  :repairsSingleDevice db/get-repairs-single-device
                  :repairsDepartment  db/get-repairs-department
                  :monthlyFuel        db/get-monthly-fuel
                  :departmentFuel     db/get-department-fuel
                  :departmentEnergy   db/get-department-energy
                  :vehicleFuel        db/get-vehicle-fuel}
          results (reduce-kv (fn [m k f]
                               (try
                                 (assoc m k (transform-rows (f)))
                                 (catch Exception e
                                   (assoc m k {:_view-error (str k) :_error (.getMessage e)}))))
                             {} queries)
          errors  (into {} (filter (fn [[_ v]] (:_error v)) results))]
      (if (seq errors)
        (json-response {:ok false
                        :error "部分视图查询失败"
                        :errors (reduce-kv (fn [m k v]
                                             (assoc m k
                                                    {:view (api-key->view k k)
                                                     :message (:_error v)}))
                                           {}
                                           errors)
                        :data (into {} (remove (fn [[_ v]] (:_error v)) results))})
        (ok results)))
    (catch Exception e (err (.getMessage e)))))

;; ─── 出库明细（支持按仓库字段过滤）───

(defn api-outbound-details-filtered [req]
  (try
    (let [wh    (get-in req [:params :warehouse])
          dept  (get-in req [:params :department])
          data  (db/get-outbound-details)
          data  (if wh  (filter #(= (:warehouse_code %) wh)  data) data)
          data  (if dept (filter #(= (:department %) dept) data) data)]
      (ok data))
    (catch Exception e (err (.getMessage e)))))

(defn api-asset-disposal-details [req]
  (try
    (ok (transform-rows (db/get-asset-disposal-details (get-in req [:params :department]))))
    (catch Exception e (err (.getMessage e)))))

(defn api-asset-inventory-details [req]
  (try
    (ok (transform-rows (db/get-asset-inventory-details (get-in req [:params :year]))))
    (catch Exception e (err (.getMessage e)))))

(defn api-asset-transfer-details [req]
  (try
    (ok (transform-rows (db/get-asset-transfer-details
                         (get-in req [:params :document-no])
                         (get-in req [:params :from-department]))))
    (catch Exception e (err (.getMessage e)))))

;; ──────────────────────────────────────────────────────────────────────
;; 静态文件服务
;; ──────────────────────────────────────────────────────────────────────

(defn serve-index
  "提供 index.html。"
  [_]
  (let [idx (clojure.java.io/resource "public/index.html")]
    (if idx
      (-> (resp/resource-response "public/index.html")
          (resp/content-type "text/html; charset=utf-8"))
      (resp/not-found "<h1>404 — index.html 未找到</h1>"))))

;; ──────────────────────────────────────────────────────────────────────
;; 路由定义
;; ──────────────────────────────────────────────────────────────────────

(defroutes app-routes
  ;; API
  (GET "/api/health"                    [] api-health)
  (GET "/api/assets/by-type"            [] api-assets-by-type)
  (GET "/api/assets/by-dept"            [] api-assets-by-dept)
  (GET "/api/depreciation"              [] api-depreciation)
  (GET "/api/annual-dynamics"           [] api-annual-dynamics)
  (GET "/api/outbound/summary"          [] api-outbound-summary)
  (GET "/api/outbound/details"          [] api-outbound-details)
  (GET "/api/outbound/details/search"   [warehouse department] api-outbound-details-filtered)
  (GET "/api/assets/disposal/details"  [department] api-asset-disposal-details)
  (GET "/api/assets/inventory/details" [year] api-asset-inventory-details)
  (GET "/api/assets/transfer/details"  [document-no from-department] api-asset-transfer-details)
  (GET "/api/repairs"                   [] api-repairs)
  (GET "/api/repairs/single-device"     [] api-repairs-single-device)
  (GET "/api/repairs/department"        [] api-repairs-department)
  (GET "/api/fuel/monthly"              [] api-monthly-fuel)
  (GET "/api/fuel/department"           [] api-department-fuel)
  (GET "/api/energy/department"          [] api-department-energy)
  (GET "/api/fuel/vehicle"              [] api-vehicle-fuel)

  ;; 静态资源
  ;; 聚合 API（必须放在静态资源之前，避免被兜底路由捕获）
  (GET "/api/all" [] api-all)

  ;; 静态资源
  (route/resources "/" {:root "public"})

  ;; SPA 兜底
  (GET "/*" [] serve-index))

;; ──────────────────────────────────────────────────────────────────────
;; 中间件
;; ──────────────────────────────────────────────────────────────────────

(def app
  (-> app-routes
      access-gate
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-not-modified))

;; ──────────────────────────────────────────────────────────────────────
;; 启动 / 停止
;; ──────────────────────────────────────────────────────────────────────

(defn start []
  (let [port (get (cfg/server-config) :port 8080)
        server (jetty/run-jetty app {:port port :join? false})]
    (println (str "🌐 服务器启动: http://127.0.0.1:" port))
    (println (str "📄 健康检查: http://127.0.0.1:" port "/api/health"))
    server))

(defn stop [server]
  (when server
    (.stop ^org.eclipse.jetty.server.Server server)
    (println "🌐 服务器已停止")))
