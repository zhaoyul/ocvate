(ns ocvate.config
  "读取 EDN 配置文件。
   默认从系统属性 conf 指定的路径读取，回退到 classpath 上的 config.edn。"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

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
