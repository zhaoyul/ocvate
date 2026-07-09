-- :name get-assets-by-type :? :*
SELECT
  asset_type        AS "asset_type",
  COUNT(*)          AS "count",
  SUM(original_value) AS "total_value",
  SUM(net_value)    AS "net_value"
FROM v_assets
GROUP BY asset_type
ORDER BY asset_type

-- :name get-assets-by-dept :? :*
SELECT
  dept_name         AS "dept_name",
  COUNT(*)          AS "asset_count",
  SUM(original_value) AS "total_value",
  SUM(net_value)    AS "net_value"
FROM v_assets
GROUP BY dept_name
ORDER BY SUM(original_value) DESC

-- :name get-depreciation-summary :? :*
SELECT
  fiscal_year       AS "fiscal_year",
  SUM(original_value) AS "original_value",
  SUM(accumulated_depr) AS "accumulated_depreciation",
  SUM(net_value)    AS "net_value",
  AVG(depr_rate)    AS "avg_depr_rate"
FROM v_assets
GROUP BY fiscal_year
ORDER BY fiscal_year

-- :name get-annual-dynamics :? :*
SELECT
  year              AS "year",
  SUM(increase)     AS "increase_amount",
  SUM(decrease)     AS "decrease_amount",
  SUM(net_increase) AS "net_increase"
FROM v_annual_dynamics
GROUP BY year
ORDER BY year

-- :name get-outbound-summary :? :*
SELECT
  part_category     AS "part_category",
  SUM(quantity)     AS "total_qty",
  SUM(amount)       AS "total_amount",
  COUNT(DISTINCT part_no) AS "part_count"
FROM v_outbound
GROUP BY part_category
ORDER BY SUM(amount) DESC

-- :name get-repair-summary :? :*
SELECT
  repair_type       AS "repair_type",
  COUNT(*)          AS "repair_count",
  SUM(cost)         AS "total_cost",
  AVG(duration_days) AS "avg_duration"
FROM v_repairs
GROUP BY repair_type
ORDER BY repair_type

-- :name get-fuel-summary :? :*
SELECT
  fuel_type         AS "fuel_type",
  SUM(volume)       AS "total_volume",
  SUM(amount)       AS "total_amount",
  COUNT(DISTINCT vehicle_no) AS "vehicle_count"
FROM v_fuel
GROUP BY fuel_type
ORDER BY fuel_type

-- :name get-assets-by-year :? :*
SELECT *
FROM v_assets
WHERE fiscal_year = :fiscal_year
ORDER BY dept_name, asset_type
