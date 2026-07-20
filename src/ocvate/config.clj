(ns ocvate.config
  "读取 EDN 配置文件。
   默认从系统属性 conf 指定的路径读取，回退到 classpath 上的 config.edn。"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def ^:private default-views
  "默认视图名映射（也用作 SQLite 测试表名）。"
  {:departments        "dept_rank"
   :assetTypes         "asset_type"
   :depreciation       "depreciation"
   :annualDynamics     "annual_dynamics"
   :outboundSummary    "outbound_summary"
   :outboundDetails    "outbound_details"
   :repairs            "repairs_single_device"
   :repairsSingleDevice "repairs_single_device"
   :repairsDepartment  "repairs_department"
   :monthlyFuel        "monthly_fuel"
   :departmentFuel     "department_fuel"
   :departmentEnergy   "department_energy"
   :vehicleFuel        "vehicle_fuel"
   :assetDisposal      "u5_fu_zpczsqdmx01"
   :assetInventory     "u5_fu_zppdqd01"
   :assetTransfer      "u5_fu_zpzyqdmx01"})

(defonce ^:dynamic *config*
  (delay
    (let [path (System/getProperty "conf" "config.edn")
          f    (io/file path)]
      (if (.exists f)
        (edn/read-string (slurp f))
        (throw (ex-info (str "配置文件未找到: " (.getAbsolutePath f))
                        {:path path}))))))

(defn config
  "获取整个配置 map。"
  []
  @*config*)

(defn db-config
  "获取数据库配置段。"
  []
  (get (config) :db))

(defn server-config
  "获取服务器配置段。"
  []
  (get (config) :server))

(defn auth-config
  "获取访问鉴权配置。"
  []
  (get (config) :auth {}))

(defn view-name
  "获取视图/表名。\n   - SQLite 始终使用默认表名（测试表 DDL 固定）。\n   - Oracle 允许 config 中 :views 段覆盖。"
  ([k] (view-name (keyword (or (:dbtype (db-config)) "oracle")) k))
  ([db-type k]
   (if (= db-type :sqlite)
     (default-views k)
     (get-in (config) [:views k] (default-views k)))))

(defn default-view-names
  "暴露默认视图名映射。"
  []
  default-views)
