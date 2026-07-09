-- =============================================================================
-- 企业资产图表分析 — Oracle 视图定义
-- =============================================================================
-- 目标: Oracle 12c+
-- 用途: 将现有业务表映射为前端所需的 10 个视图
-- 使用: 将 FROM 子句中的表名替换为实际的业务表名
-- 执行: sqlplus scott/tiger@XE @sql/oracle-views.sql
-- =============================================================================

-- ============================================================
-- 1. 部门资产统计（前端: departments）
-- ============================================================
CREATE OR REPLACE VIEW dept_rank AS
SELECT
  department              AS department,
  asset_count             AS asset_count,
  ratio                   AS ratio,
  idle_asset_count        AS idle_asset_count,
  fixed_asset_count       AS fixed_asset_count,
  fixed_asset_original_value AS fixed_asset_original_value
FROM your_department_table;   -- ← 替换为实际表名

-- ============================================================
-- 2. 资产类型统计（前端: assetTypes）
-- ============================================================
CREATE OR REPLACE VIEW asset_type AS
SELECT
  category                AS category,
  asset_count             AS asset_count,
  count_ratio             AS count_ratio,
  value                   AS value,
  value_ratio             AS value_ratio
FROM your_asset_type_table;  -- ← 替换为实际表名

-- ============================================================
-- 3. 折旧到期统计（前端: depreciation）
-- ============================================================
CREATE OR REPLACE VIEW depreciation AS
SELECT
  category                AS category,
  year                    AS year,
  depreciation_count      AS depreciation_count,
  original_value          AS original_value
FROM your_depreciation_table;  -- ← 替换为实际表名

-- ============================================================
-- 4. 年度资产动态（前端: annualDynamics）
-- ============================================================
CREATE OR REPLACE VIEW annual_dynamics AS
SELECT
  year                    AS year,
  added_count             AS added_count,
  added_value             AS added_value,
  pending_count           AS pending_count,
  pending_value           AS pending_value
FROM your_annual_dynamics_table;  -- ← 替换为实际表名

-- ============================================================
-- 5. 出库汇总（前端: outboundSummary）
-- ============================================================
CREATE OR REPLACE VIEW outbound_summary AS
SELECT
  year                    AS year,
  warehouse_code          AS warehouse_code,
  warehouse_name          AS warehouse_name,
  department_code         AS department_code,
  department              AS department,
  amount                  AS amount,
  quantity                AS quantity,
  yoy                     AS yoy,
  mom                     AS mom
FROM your_outbound_summary_table;  -- ← 替换为实际表名

-- ============================================================
-- 6. 出库明细（前端: outboundDetails）
-- ============================================================
CREATE OR REPLACE VIEW outbound_details AS
SELECT
  warehouse_code          AS warehouse_code,
  warehouse_name          AS warehouse_name,
  department_code         AS department_code,
  department              AS department,
  ticket_no               AS ticket_no,
  spare_code              AS spare_code,
  spare_name              AS spare_name,
  quantity                AS quantity,
  unit                    AS unit,
  class01                 AS class01,
  class02                 AS class02,
  class03                 AS class03,
  unit_price              AS unit_price,
  total_price             AS total_price,
  time                    AS time
FROM your_outbound_details_table;  -- ← 替换为实际表名

-- ============================================================
-- 7. 单台设备维修费用分析（前端: repairs / repairsSingleDevice）
-- ============================================================
CREATE OR REPLACE VIEW repairs_single_device AS
SELECT
  year                    AS year,
  month                   AS month,
  department_code         AS department_code,
  department              AS department,
  asset_code              AS asset_code,
  equipment_name          AS equipment_name,
  plate_no                AS plate_no,
  total_cost              AS total_cost
FROM your_repairs_single_device_table;  -- ← 替换为实际表名

-- ============================================================
-- 8. 部门设备维修费用分析（前端: repairsDepartment）
-- ============================================================
CREATE OR REPLACE VIEW repairs_department AS
SELECT
  year                    AS year,
  month                   AS month,
  department_code         AS department_code,
  department              AS department,
  total_cost              AS total_cost,
  repair_count            AS repair_count
FROM your_repairs_department_table;  -- ← 替换为实际表名

-- ============================================================
-- 9. 月度油耗（前端: monthlyFuel）
-- ============================================================
CREATE OR REPLACE VIEW monthly_fuel AS
SELECT
  year                    AS year,
  month                   AS month,
  fuel_volume             AS fuel_volume
FROM your_monthly_fuel_table;  -- ← 替换为实际表名

-- ============================================================
-- 10. 部门油耗（前端: departmentFuel）
-- ============================================================
CREATE OR REPLACE VIEW department_fuel AS
SELECT
  year                    AS year,
  month                   AS month,
  department_code         AS department_code,
  department              AS department,
  fuel_type_code          AS fuel_type_code,
  fuel_type               AS fuel_type,
  fuel_volume             AS fuel_volume,
  total_ratio             AS total_ratio
FROM your_department_fuel_table;  -- ← 替换为实际表名

-- ============================================================
-- 11. 部门能耗（前端: departmentEnergy，各部门车辆能耗统计表）
-- ============================================================
CREATE OR REPLACE VIEW department_energy AS
SELECT
  year                    AS year,
  month                   AS month,
  department              AS department,
  fuel_volume             AS fuel_volume,
  energy_type             AS energy_type,
  energy_method           AS energy_method,
  energy                  AS energy,
  mom                     AS mom,
  yoy                     AS yoy
FROM your_department_energy_table;  -- ← 替换为实际表名

-- ============================================================
-- 12. 单车油耗（前端: vehicleFuel）
-- ============================================================
CREATE OR REPLACE VIEW vehicle_fuel AS
SELECT
  year                    AS year,
  month                   AS month,
  asset_code              AS asset_code,
  equipment_name          AS equipment_name,
  plate_no                AS plate_no,
  fuel_volume             AS fuel_volume
FROM your_vehicle_fuel_table;  -- ← 替换为实际表名

-- ============================================================
-- 授权（视需要放开）
-- ============================================================
-- GRANT SELECT ON dept_rank TO 用户名;
-- GRANT SELECT ON asset_type TO 用户名;
-- GRANT SELECT ON depreciation TO 用户名;
-- GRANT SELECT ON annual_dynamics TO 用户名;
-- GRANT SELECT ON outbound_summary TO 用户名;
-- GRANT SELECT ON outbound_details TO 用户名;
-- GRANT SELECT ON repairs TO 用户名;
-- GRANT SELECT ON monthly_fuel TO 用户名;
-- GRANT SELECT ON department_fuel TO 用户名;
-- GRANT SELECT ON vehicle_fuel TO 用户名;

COMMIT;
