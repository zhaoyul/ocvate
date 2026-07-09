(ns ocvate.db
  "数据库连接池管理与查询函数。
   支持 Oracle 和 SQLite 两种数据库后端。"
  (:require [ocvate.config :as cfg]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))

(defn- db-type [] (keyword (or (:dbtype (cfg/db-config)) "oracle")))
(defn- oracle-url [c] (str "jdbc:oracle:thin:@" (:host c) ":" (:port c) "/" (:dbname c)))
(defn- sqlite-url [c] (str "jdbc:sqlite:" (:dbname c)))

(defn- make-oracle-datasource [c]
  (let [h (HikariConfig.)]
    (.setJdbcUrl h (oracle-url c)) (.setUsername h (:user c)) (.setPassword h (:password c))
    (.setDriverClassName h "oracle.jdbc.OracleDriver")
    (.setMaximumPoolSize h (or (:pool-size c) 10))
    (.setConnectionTimeout h (or (:connection-timeout-ms c) 30000))
    (.setIdleTimeout h (or (:idle-timeout-ms c) 600000))
    (.setMaxLifetime h (or (:max-lifetime-ms c) 1800000))
    (.setAutoCommit h false) (HikariDataSource. h)))

(defn- make-sqlite-datasource [c]
  (let [h (HikariConfig.)]
    (.setJdbcUrl h (sqlite-url c)) (.setDriverClassName h "org.sqlite.JDBC")
    (.setMaximumPoolSize h 1) (.setAutoCommit h true) (HikariDataSource. h)))

(defn make-datasource [c]
  (case (keyword (:dbtype c)) :sqlite (make-sqlite-datasource c) (make-oracle-datasource c)))

(defonce ^:dynamic *datasource*
  (delay (let [c (cfg/db-config) k (keyword (:dbtype c "oracle"))]
           (println (str "🔌 " (name k) " — " (if (= k :sqlite) (:dbname c) (str (:host c) ":" (:port c) "/" (:dbname c)))))
           (make-datasource c))))
(defn datasource [] @*datasource*)

(defn- with-conn* [f]
  (with-open [c (.getConnection ^HikariDataSource (datasource))] (f {:connection c})))

(defn query [s & p]
  (with-conn* (fn [db] (apply jdbc/query db (if (sequential? (first p)) (concat [s] p) (conj p s))))))

(defn execute! [s & p]
  (with-conn* (fn [db] (apply jdbc/execute! db (if (sequential? (first p)) (concat [s] p) (conj p s))))))

;; ── 具名查询 ──
(defn get-assets-by-type ([] (get-assets-by-type {})) ([_]
  (query "SELECT * FROM asset_type ORDER BY asset_count DESC")))
(defn get-assets-by-dept ([] (get-assets-by-dept {})) ([_]
  (query "SELECT * FROM dept_rank ORDER BY asset_count DESC")))
(defn get-depreciation-summary ([] (get-depreciation-summary {})) ([_]
  (query "SELECT * FROM depreciation ORDER BY year")))
(defn get-annual-dynamics ([] (get-annual-dynamics {})) ([_]
  (query "SELECT * FROM annual_dynamics ORDER BY year")))
(defn get-outbound-summary ([] (get-outbound-summary {})) ([_]
  (query "SELECT * FROM outbound_summary ORDER BY year, warehouse_code")))
(defn get-outbound-details ([] (get-outbound-details {})) ([_]
  (query "SELECT * FROM outbound_details ORDER BY time DESC")))
(defn get-repairs-single-device ([] (get-repairs-single-device {})) ([_]
  (query "SELECT * FROM repairs_single_device ORDER BY year, month")))
(defn get-repairs-department ([] (get-repairs-department {})) ([_]
  (query "SELECT * FROM repairs_department ORDER BY year, month")))
(defn get-repairs ([] (get-repairs {})) ([_]
  (query "SELECT * FROM repairs_single_device ORDER BY year, month")))
(defn get-monthly-fuel ([] (get-monthly-fuel {})) ([_]
  (query "SELECT * FROM monthly_fuel ORDER BY year, month")))
(defn get-department-fuel ([] (get-department-fuel {})) ([_]
  (query "SELECT * FROM department_fuel ORDER BY year, month")))
(defn get-department-energy ([] (get-department-energy {})) ([_]
  (query "SELECT * FROM department_energy ORDER BY year, month")))
(defn get-vehicle-fuel ([] (get-vehicle-fuel {})) ([_]
  (query "SELECT * FROM vehicle_fuel ORDER BY year, month")))

(defn get-all-data []
  {:departments (get-assets-by-dept) :assetTypes (get-assets-by-type)
   :depreciation (get-depreciation-summary) :annualDynamics (get-annual-dynamics)
   :outboundSummary (get-outbound-summary) :outboundDetails (get-outbound-details)
   :repairs (get-repairs) :repairsSingleDevice (get-repairs-single-device) :repairsDepartment (get-repairs-department) :monthlyFuel (get-monthly-fuel)
   :departmentFuel (get-department-fuel) :departmentEnergy (get-department-energy) :vehicleFuel (get-vehicle-fuel)})

;; ── SQLite 初始化（匹配前端 createDefaultData 的机场场景数据）──
(def sqlite-init-ddl
  ["CREATE TABLE IF NOT EXISTS dept_rank (department TEXT,asset_count INTEGER,ratio REAL,idle_asset_count INTEGER,fixed_asset_count INTEGER,fixed_asset_original_value REAL)"
   "CREATE TABLE IF NOT EXISTS asset_type (category TEXT,asset_count INTEGER,count_ratio REAL,value REAL,value_ratio REAL)"
   "CREATE TABLE IF NOT EXISTS depreciation (category TEXT,year INTEGER,depreciation_count INTEGER,original_value REAL)"
   "CREATE TABLE IF NOT EXISTS annual_dynamics (year INTEGER,added_count INTEGER,added_value REAL,pending_count INTEGER,pending_value REAL)"
   "CREATE TABLE IF NOT EXISTS outbound_summary (year INTEGER,warehouse_code TEXT,warehouse_name TEXT,department_code TEXT,department TEXT,amount REAL,quantity INTEGER,yoy REAL,mom REAL)"
   "CREATE TABLE IF NOT EXISTS outbound_details (warehouse_code TEXT,warehouse_name TEXT,department_code TEXT,department TEXT,ticket_no TEXT,spare_code TEXT,spare_name TEXT,quantity INTEGER,unit TEXT,class01 TEXT,class02 TEXT,class03 TEXT,unit_price REAL,total_price REAL,time TEXT)"
   "CREATE TABLE IF NOT EXISTS repairs_single_device (year INTEGER,month INTEGER,department_code TEXT,department TEXT,asset_code TEXT,equipment_name TEXT,plate_no TEXT,total_cost REAL)"
   "CREATE TABLE IF NOT EXISTS repairs_department (year INTEGER,month INTEGER,department_code TEXT,department TEXT,total_cost REAL,repair_count INTEGER)"
   "CREATE TABLE IF NOT EXISTS monthly_fuel (year INTEGER,month INTEGER,fuel_volume REAL)"
   "CREATE TABLE IF NOT EXISTS department_fuel (year INTEGER,month INTEGER,department_code TEXT,department TEXT,fuel_type_code TEXT,fuel_type TEXT,fuel_volume REAL,total_ratio REAL)"
   "CREATE TABLE IF NOT EXISTS department_energy (year INTEGER,month INTEGER,department TEXT,fuel_volume REAL,energy_type TEXT,energy_method TEXT,energy REAL,mom REAL,yoy REAL)"
   "CREATE TABLE IF NOT EXISTS vehicle_fuel (year INTEGER,month INTEGER,asset_code TEXT,equipment_name TEXT,plate_no TEXT,fuel_volume REAL)"])

(def sqlite-init-data
  [(str "DELETE FROM dept_rank")
   (str "INSERT INTO dept_rank VALUES "
        "('信息管理部',27190,0.199,100,111,887),('公共区管理部',22922,0.168,101,104,760),"
        "('动力能源部',22697,0.166,102,116,920),('航站区管理部',21147,0.155,103,120,816),"
        "('商旅管理公司-君廷酒店',12142,0.089,104,72,310),('航空安保部',6682,0.049,105,64,280),"
        "('飞行区管理部',3928,0.029,106,56,219),('公共交通管理部',3095,0.023,107,47,185),"
        "('消防救援部',2890,0.021,108,41,166),('商贸管理公司-青岛环亚公司(凯撒湾）',1900,0.014,109,33,142),"
        "('运行指挥中心（总值班室）',1880,0.014,110,31,128),('地面服务部',1788,0.013,111,28,119),"
        "('公共交通管理部-机场公安',1544,0.011,112,0,0),('商贸管理公司-商务发展公司',1226,0.009,113,0,0),"
        "('机务维修公司',1188,0.009,114,0,0),('商贸管理公司',910,0.007,115,0,0),"
        "('医疗急救中心',598,0.004,116,0,0),('人力资源部',520,0.004,117,0,0),"
        "('办公室',486,0.004,118,0,0),('资产管理部',367,0.003,119,0,0),"
        "('综合管理部',268,0.002,120,0,0),('财务管理部',144,0.001,121,0,0),"
        "('空防保卫部',112,0.001,122,0,0),('管理公司办公室',103,0.001,123,0,0),"
        "('宣传部',102,0.001,124,0,0),('工会办公室',98,0.001,125,0,0),"
        "('集团纪委',73,0.001,126,0,0),('航空市场营销委员会',73,0.001,127,0,0),"
        "('新航服公司',71,0.001,128,0,0),('安全管理部',67,0,129,0,0),"
        "('未出库',64,0,130,0,0),('经营管理部',52,0,131,0,0),"
        "('党委组织部',48,0,132,0,0),('法务审计部',38,0,133,0,0),"
        "('服务管理部',37,0,134,0,0),('职工食堂管理工作领导小组',35,0,135,0,0),"
        "('总工办公室',33,0,136,0,0),('企划研究部',33,0,137,0,0),"
        "('航服公司（原商旅管理公司）',23,0,138,0,0),('航服公司',21,0,139,0,0),"
        "('航空城开发建设指挥部',21,0,140,0,0),('政策研究室',19,0,141,0,0),"
        "('督查办公室',18,0,142,0,0),('急救部',15,0,143,0,0),('科技创新中心',12,0,144,0,0),"
        "('新航服综合管理部',7,0,145,0,0),('考核办公室',5,0,146,0,0),"
        "('职工宿舍小组',5,0,147,0,0),('停车场管理公司',5,0,148,0,0),"
        "('酒店管理部',4,0,149,0,0),('航服办公室（原商旅公司办公室）',1,0,150,0,0)")
   (str "DELETE FROM asset_type")
   (str "INSERT INTO asset_type VALUES "
        "('不动产类',2305,0.017,237587.52,0.708),('不动产附着物类',61345,0.451,65564.36,0.195),"
        "('可移动类',47964,0.352,20954.47,0.063),('车辆及附属类',1535,0.011,2701.41,0.008),"
        "('隐蔽设备类',22360,0.164,8550.01,0.026)")
   (str "DELETE FROM depreciation")
   (str "INSERT INTO depreciation VALUES "
        "('办公类设备',2024,1,4.08),('办公类设备',2025,122,1105.27),('办公类设备',2026,345,634.81),"
        "('不动产附着物类',2027,38886,160997.19),('可移动类',2028,6025,8772.33),"
        "('车辆及附属类',2029,24901,86844.24),('办公类设备',2030,13693,17174.03),"
        "('不动产类',2031,18449,166801.92),('隐蔽设备类',2032,1626,2586.5),"
        "('可移动类',2033,1596,3204.16),('车辆及附属类',2034,375,1702.39),"
        "('办公类设备',2035,1463,2988.09),('不动产附着物类',2036,24766,314166.07),"
        "('车辆及附属类',2037,572,8337.6),('办公类设备',2038,168,513.27),"
        "('可移动类',2039,47,369.22),('隐蔽设备类',2040,1191,1190.89)")
   (str "DELETE FROM annual_dynamics")
   (str "INSERT INTO annual_dynamics VALUES "
        "(2019,1240,3840.5,520,1240.3),(2020,1850,5920.2,680,1980.7),"
        "(2021,2230,7340.8,890,2670.2),(2022,1980,6560.3,1040,3120.5),"
        "(2023,2670,9850.6,1280,3940.1),(2024,3100,12030.4,1420,4590.6),"
        "(2025,3450,13800,1650,5350),(2026,3720,15250.5,1820,6120.8)")
   (str "DELETE FROM outbound_summary")
   (str "INSERT INTO outbound_summary VALUES "
        "(2026,'01','综合物资库','D001','飞行区管理部',186.2,421,0.082,0.031),"
        "(2026,'01','综合物资库','D002','动力能源部',155.8,336,0.041,-0.018),"
        "(2026,'01','综合物资库','D003','航站区管理部',129.6,298,0.065,0.022),"
        "(2026,'01','综合物资库','D004','公共区管理部',118.4,244,-0.012,0.017),"
        "(2026,'02','机务备件库','D005','机务维修公司',96.5,187,0.091,0.044),"
        "(2025,'01','综合物资库','D001','飞行区管理部',171.1,389,0.026,0.015)")
   (str "DELETE FROM outbound_details")
   (str "INSERT INTO outbound_details VALUES "
        "('01','综合物资库','D001','飞行区管理部','LL-2026-11','BJ-1108','过滤器',5,'件','维修备件','常用备件','综合',1.15,5.75,'2026-03-11'),"
        "('01','综合物资库','D001','飞行区管理部','LL-2026-12','BJ-1208','传感器',6,'件','维修备件','常用备件','综合',1.5,9.0,'2026-04-12'),"
        "('01','综合物资库','D001','飞行区管理部','LL-2026-13','BJ-1308','照明组件',7,'件','维修备件','常用备件','综合',1.85,12.95,'2026-05-13'),"
        "('01','综合物资库','D002','动力能源部','LL-2026-21','BJ-2108','过滤器',6,'件','维修备件','常用备件','综合',1.15,6.9,'2026-03-21'),"
        "('01','综合物资库','D002','动力能源部','LL-2026-22','BJ-2208','传感器',7,'件','维修备件','常用备件','综合',1.5,10.5,'2026-04-22'),"
        "('01','综合物资库','D002','动力能源部','LL-2026-23','BJ-2308','照明组件',8,'件','维修备件','常用备件','综合',1.85,14.8,'2026-05-23'),"
        "('01','综合物资库','D003','航站区管理部','LL-2026-31','BJ-3108','过滤器',7,'件','维修备件','常用备件','综合',1.15,8.05,'2026-03-31'),"
        "('01','综合物资库','D003','航站区管理部','LL-2026-32','BJ-3208','传感器',8,'件','维修备件','常用备件','综合',1.5,12.0,'2026-04-32'),"
        "('01','综合物资库','D003','航站区管理部','LL-2026-33','BJ-3308','照明组件',9,'件','维修备件','常用备件','综合',1.85,16.65,'2026-05-33'),"
        "('01','综合物资库','D004','公共区管理部','LL-2026-41','BJ-4108','过滤器',8,'件','维修备件','常用备件','综合',1.15,9.2,'2026-03-41'),"
        "('01','综合物资库','D004','公共区管理部','LL-2026-42','BJ-4208','传感器',9,'件','维修备件','常用备件','综合',1.5,13.5,'2026-04-42'),"
        "('01','综合物资库','D004','公共区管理部','LL-2026-43','BJ-4308','照明组件',10,'件','维修备件','常用备件','综合',1.85,18.5,'2026-05-43'),"
        "('02','机务备件库','D005','机务维修公司','LL-2026-51','BJ-5108','过滤器',9,'件','维修备件','常用备件','综合',1.15,10.35,'2026-03-51'),"
        "('02','机务备件库','D005','机务维修公司','LL-2026-52','BJ-5208','传感器',10,'件','维修备件','常用备件','综合',1.5,15.0,'2026-04-52'),"
        "('02','机务备件库','D005','机务维修公司','LL-2026-53','BJ-5308','照明组件',11,'件','维修备件','常用备件','综合',1.85,20.35,'2026-05-53'),"
        "('01','综合物资库','D001','飞行区管理部','LL-2025-61','BJ-6108','过滤器',10,'件','维修备件','常用备件','综合',1.15,11.5,'2025-03-61'),"
        "('01','综合物资库','D001','飞行区管理部','LL-2025-62','BJ-6208','传感器',11,'件','维修备件','常用备件','综合',1.5,16.5,'2025-04-62'),"
        "('01','综合物资库','D001','飞行区管理部','LL-2025-63','BJ-6308','照明组件',12,'件','维修备件','常用备件','综合',1.85,22.2,'2025-05-63')")
   (str "DELETE FROM repairs_single_device")
   (str "DELETE FROM repairs_department")
   (str "INSERT INTO repairs_single_device VALUES "
        "(2026,1,'D003','航站区管理部','DEV-1001','大型行李安检机','',5000),"
        "(2026,2,'D003','航站区管理部','DEV-1001','大型行李安检机','',5000),"
        "(2026,3,'D003','航站区管理部','DEV-1001','大型行李安检机','',5400),"
        "(2026,4,'D003','航站区管理部','DEV-1001','大型行李安检机','',6120),"
        "(2026,5,'D003','航站区管理部','DEV-1001','大型行李安检机','',6850),"
        "(2026,6,'D003','航站区管理部','DEV-1001','大型行李安检机','',5980),"
        "(2026,1,'D006','公共交通管理部','DEV-2056','摆渡车','鲁B-3112',8500),"
        "(2026,2,'D006','公共交通管理部','DEV-2056','摆渡车','鲁B-3112',8600),"
        "(2026,3,'D006','公共交通管理部','DEV-2056','摆渡车','鲁B-3112',8950),"
        "(2026,4,'D006','公共交通管理部','DEV-2056','摆渡车','鲁B-3112',9800),"
        "(2026,5,'D006','公共交通管理部','DEV-2056','摆渡车','鲁B-3112',10800),"
        "(2026,6,'D006','公共交通管理部','DEV-2056','摆渡车','鲁B-3112',10200),"
        "(2026,1,'D002','动力能源部','DEV-3789','中央空调机组','',14900),"
        "(2026,2,'D002','动力能源部','DEV-3789','中央空调机组','',14100),"
        "(2026,3,'D002','动力能源部','DEV-3789','中央空调机组','',15800),"
        "(2026,4,'D002','动力能源部','DEV-3789','中央空调机组','',16780),"
        "(2026,5,'D002','动力能源部','DEV-3789','中央空调机组','',17950),"
        "(2026,6,'D002','动力能源部','DEV-3789','中央空调机组','',17250),"
        "(2026,1,'D001','飞行区管理部','DEV-4491','跑道巡检车','鲁U-8890',6100),"
        "(2026,2,'D001','飞行区管理部','DEV-4491','跑道巡检车','鲁U-8890',6400),"
        "(2026,3,'D001','飞行区管理部','DEV-4491','跑道巡检车','鲁U-8890',6900),"
        "(2026,4,'D001','飞行区管理部','DEV-4491','跑道巡检车','鲁U-8890',7200),"
        "(2026,5,'D001','飞行区管理部','DEV-4491','跑道巡检车','鲁U-8890',7600),"
        "(2026,6,'D001','飞行区管理部','DEV-4491','跑道巡检车','鲁U-8890',7900)")
   (str "DELETE FROM repairs_department")
   (str "INSERT INTO repairs_department VALUES "
        "(2026,1,'D003','航站区管理部',5000,1),(2026,2,'D003','航站区管理部',5000,1),"
        "(2026,3,'D003','航站区管理部',5400,1),(2026,4,'D003','航站区管理部',6120,1),"
        "(2026,5,'D003','航站区管理部',6850,1),(2026,6,'D003','航站区管理部',5980,1),"
        "(2026,1,'D006','公共交通管理部',8500,1),(2026,2,'D006','公共交通管理部',8600,1),"
        "(2026,3,'D006','公共交通管理部',8950,1),(2026,4,'D006','公共交通管理部',9800,1),"
        "(2026,5,'D006','公共交通管理部',10800,1),(2026,6,'D006','公共交通管理部',10200,1),"
        "(2026,1,'D002','动力能源部',14900,1),(2026,2,'D002','动力能源部',14100,1),"
        "(2026,3,'D002','动力能源部',15800,1),(2026,4,'D002','动力能源部',16780,1),"
        "(2026,5,'D002','动力能源部',17950,1),(2026,6,'D002','动力能源部',17250,1),"
        "(2026,1,'D001','飞行区管理部',6100,1),(2026,2,'D001','飞行区管理部',6400,1),"
        "(2026,3,'D001','飞行区管理部',6900,1),(2026,4,'D001','飞行区管理部',7200,1),"
        "(2026,5,'D001','飞行区管理部',7600,1),(2026,6,'D001','飞行区管理部',7900,1)")
   (str "DELETE FROM monthly_fuel")
   (str "INSERT INTO monthly_fuel VALUES "
        "(2026,1,16200),(2026,2,15800),(2026,3,17100),(2026,4,16800),"
        "(2026,5,17500),(2026,6,18120),(2025,1,15030),(2025,2,14880)")
   (str "DELETE FROM department_fuel")
   (str "INSERT INTO department_fuel VALUES "
        "(2026,6,'D001','飞行区管理部','GAS','汽油',6040.7,0.3445),"
        "(2026,6,'D002','动力能源部','DIE','柴油',2450.4,0.1398),"
        "(2026,6,'D003','航站区管理部','GAS','汽油',3100.2,0.1769),"
        "(2026,6,'D004','公共区管理部','DIE','柴油',2890.1,0.1649),"
        "(2026,6,'D005','机务维修公司','DIE','柴油',1453.5,0.0829),"
        "(2026,6,'D006','办公室','GAS','汽油',1736.0,0.0991),"
        "(2026,5,'D001','飞行区管理部','GAS','汽油',5810.2,0.331),"
        "(2026,5,'D002','动力能源部','DIE','柴油',2310.9,0.132)")
   (str "DELETE FROM department_energy")
   (str "INSERT INTO department_energy VALUES "
        "(2026,6,'飞行区管理部',6040.7,'汽油','百公里',13.77,0.0412,0.0412),"
        "(2026,6,'动力能源部',2450.4,'柴油','台班',11.22,-0.018,0.033),"
        "(2026,6,'航站区管理部',3100.2,'汽油','百公里',12.91,0.021,0.052),"
        "(2026,6,'公共区管理部',2890.1,'柴油','台班',10.44,0.038,-0.014),"
        "(2026,6,'机务维修公司',1453.5,'柴油','台班',9.66,0.012,0.027),"
        "(2026,6,'办公室',1736.0,'汽油','百公里',8.92,-0.011,0.019),"
        "(2026,5,'飞行区管理部',5810.2,'汽油','百公里',13.14,0.018,0.031),"
        "(2026,5,'动力能源部',2310.9,'柴油','台班',10.98,-0.009,0.023)")
   (str "DELETE FROM vehicle_fuel")
   (str "INSERT INTO vehicle_fuel (year, month, asset_code, equipment_name, plate_no, fuel_volume) VALUES "
        "(2026,1,'CAR-001','通勤大巴','鲁B-A123',188),(2026,2,'CAR-001','通勤大巴','鲁B-A123',195),"
        "(2026,3,'CAR-001','通勤大巴','鲁B-A123',182),(2026,4,'CAR-001','通勤大巴','鲁B-A123',215),"
        "(2026,5,'CAR-001','通勤大巴','鲁B-A123',228),(2026,6,'CAR-001','通勤大巴','鲁B-A123',235),"
        "(2026,1,'CAR-002','贵宾中巴','鲁U-E567',115),(2026,2,'CAR-002','贵宾中巴','鲁U-E567',120),"
        "(2026,3,'CAR-002','贵宾中巴','鲁U-E567',108),(2026,4,'CAR-002','贵宾中巴','鲁U-E567',134),"
        "(2026,5,'CAR-002','贵宾中巴','鲁U-E567',146),(2026,6,'CAR-002','贵宾中巴','鲁U-E567',152),"
        "(2026,1,'CAR-003','巡逻皮卡','鲁B-F890',80),(2026,2,'CAR-003','巡逻皮卡','鲁B-F890',84),"
        "(2026,3,'CAR-003','巡逻皮卡','鲁B-F890',78),(2026,4,'CAR-003','巡逻皮卡','鲁B-F890',98),"
        "(2026,5,'CAR-003','巡逻皮卡','鲁B-F890',105),(2026,6,'CAR-003','巡逻皮卡','鲁B-F890',109),"
        "(2026,1,'CAR-004','应急保障车','',66),(2026,2,'CAR-004','应急保障车','',70),"
        "(2026,3,'CAR-004','应急保障车','',68),(2026,4,'CAR-004','应急保障车','',76),"
        "(2026,5,'CAR-004','应急保障车','',82),(2026,6,'CAR-004','应急保障车','',85)")])

(defn init-sqlite! []
  (when (= (db-type) :sqlite)
    (println "🗄️  初始化 SQLite 测试数据 (匹配前端展示)...")
    (with-conn* (fn [db]
      (doseq [sql (concat sqlite-init-ddl sqlite-init-data)]
        (try (jdbc/execute! db [sql])
             (catch Exception e
               (when-not (str/includes? (.getMessage e) "already exists")
                 (println "  ⚠️" (.getMessage e))))))))
    (println "✅ SQLite 测试数据已就绪")))

(defn stop! []
  (when (and (realized? *datasource*) @*datasource*)
    (println "🔌 关闭数据库连接池")
    (.close ^HikariDataSource @*datasource*)))
