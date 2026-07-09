-- =============================================================================
-- 企业资产图表分析 — Oracle 测试用建表脚本
-- =============================================================================
-- 仅用于测试环境创建临时表。生产环境请使用 oracle-views.sql。
-- =============================================================================

CREATE TABLE dept_rank (
    department              VARCHAR2(200),
    asset_count             NUMBER(10),
    ratio                   NUMBER(8,6),
    idle_asset_count        NUMBER(10),
    fixed_asset_count       NUMBER(10),
    fixed_asset_original_value NUMBER(15,2)
);

CREATE TABLE asset_type (
    category                VARCHAR2(100),
    asset_count             NUMBER(10),
    count_ratio             NUMBER(8,6),
    value                   NUMBER(15,2),
    value_ratio             NUMBER(8,6)
);

CREATE TABLE depreciation (
    category                VARCHAR2(100),
    year                    NUMBER(4),
    depreciation_count      NUMBER(10),
    original_value          NUMBER(15,2)
);

CREATE TABLE annual_dynamics (
    year                    NUMBER(4),
    added_count             NUMBER(10),
    added_value             NUMBER(15,2),
    pending_count           NUMBER(10),
    pending_value           NUMBER(15,2)
);

CREATE TABLE outbound_summary (
    year                    NUMBER(4),
    warehouse_code          VARCHAR2(20),
    warehouse_name          VARCHAR2(100),
    department_code         VARCHAR2(20),
    department              VARCHAR2(100),
    amount                  NUMBER(15,2),
    quantity                NUMBER(10),
    yoy                     NUMBER(8,6),
    mom                     NUMBER(8,6)
);

CREATE TABLE outbound_details (
    warehouse_code          VARCHAR2(20),
    warehouse_name          VARCHAR2(100),
    department_code         VARCHAR2(20),
    department              VARCHAR2(100),
    ticket_no               VARCHAR2(50),
    spare_code              VARCHAR2(50),
    spare_name              VARCHAR2(200),
    quantity                NUMBER(10),
    unit                    VARCHAR2(20),
    class01                 VARCHAR2(50),
    class02                 VARCHAR2(50),
    class03                 VARCHAR2(50),
    unit_price              NUMBER(15,4),
    total_price             NUMBER(15,4),
    time                    VARCHAR2(20)
);

CREATE TABLE repairs (
    year                    NUMBER(4),
    month                   NUMBER(2),
    department_code         VARCHAR2(20),
    department              VARCHAR2(100),
    asset_code              VARCHAR2(50),
    equipment_name          VARCHAR2(200),
    plate_no                VARCHAR2(50),
    total_cost              NUMBER(15,2)
);

CREATE TABLE monthly_fuel (
    year                    NUMBER(4),
    month                   NUMBER(2),
    fuel_volume             NUMBER(15,2)
);

CREATE TABLE department_fuel (
    year                    NUMBER(4),
    month                   NUMBER(2),
    department_code         VARCHAR2(20),
    department              VARCHAR2(100),
    fuel_type_code          VARCHAR2(50),
    fuel_type               VARCHAR2(50),
    fuel_volume             NUMBER(15,2),
    total_ratio             NUMBER(8,6),
    energy_type             VARCHAR2(50),
    energy_method           VARCHAR2(50),
    energy                  NUMBER(15,4),
    mom                     NUMBER(8,6),
    yoy                     NUMBER(8,6)
);

CREATE TABLE vehicle_fuel (
    year                    NUMBER(4),
    month                   NUMBER(2),
    asset_code              VARCHAR2(50),
    equipment_name          VARCHAR2(200),
    plate_no                VARCHAR2(50),
    fuel_volume             NUMBER(15,2)
);

COMMIT;
