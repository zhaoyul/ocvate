# 企业资产图表分析 — 客户说明文档

## 1. 系统概述

企业资产图表分析系统提供资产分布、出库分析、设备维修、油耗分析等核心业务看板，同时支持资产处置申请单、资产盘点单、资产转移单三张业务明细表的查询与 CSV 导出。

---

## 2. 固定访问密钥

生产环境（Oracle）访问需携带密钥：

```
key=69a09523e8ef7f221db62df835a6998781a10c9aee973d671cbd124409fc5c4e
```

**使用方式：** 主系统跳转时附加在 URL 查询参数中:

```
http://<host>:<port>/?key=69a09523e8ef7f221db62df835a6998781a10c9aee973d671cbd124409fc5c4e
```

验证通过后系统会设置会话 Cookie（`ocvate_access`，有效期 8 小时），后续访问无须重复携带密钥。

**说明：** SQLite 测试环境直接放行，不校验密钥。

---

## 3. 页面显示控制方式

通过 URL 查询参数 `pages`（或 `page`）控制可见页面，多个页码以英文逗号分隔。

**页面编号映射：**

| 页面编号 | 页面名称 | 页面 Key |
|---------|---------|---------|
| 1       | 资产    | `assets` |
| 2       | 出库    | `outbound` |
| 3       | 维修    | `repairs` |
| 4       | 油耗    | `fuel` |

**示例：**

- `?pages=1` — 仅显示"资产"页
- `?pages=1,2,3` — 显示"资产"、"出库"、"维修"三页（隐藏"油耗"）
- `?pages=1,2,3,4` — 显示全部页面
- 不传 `pages` 参数时，默认显示全部页面

**组合使用：**

```
http://<host>:<port>/?key=<密钥>&pages=1,2,3,4
```

---

## 4. 三张业务明细表

| 序号 | 表名（Oracle）                 | 逻辑名称         | 系统 Key            | CSV 导出文件名         |
|------|------------------------------|----------------|------------------|---------------------|
| 1    | `EAMPROD.u5_fu_zpczsqdmx01` | 资产处置申请单明细 | `assetDisposal`  | 资产处置申请单明细.csv |
| 2    | `EAMPROD.u5_fu_zppdqd01`    | 资产盘点单明细   | `assetInventory` | 资产盘点单明细.csv     |
| 3    | `EAMPROD.u5_fu_zpzyqdmx01`  | 资产转移单明细   | `assetTransfer`  | 资产转移单明细.csv     |

> 说明：三张视图位于 Oracle 的 `EAMPROD` Schema 下。应用数据库登录账号可以是客户环境中具备查询权限的账号，代码通过上述带 Schema 的完整视图名读取数据；数据库连接地址、服务名、用户名和密码不在本次视图名称变更范围内。

---

## 5. 明细下载筛选规则

在“资产一张表”底部的“资产业务明细下载”区域，输入或选择条件后点击“下载 CSV”，系统会按条件查询对应 Oracle 视图并下载本地 CSV 文件。

| 明细类型 | 筛选条件 | 对应字段 | 说明 |
|---------|---------|---------|------|
| 资产处置申请单明细 | 使用部门 | `dept_desc` | 按部门描述筛选；选择“全部”时下载全部数据 |
| 资产盘点单明细 | 年份 | `check_year` | 支持 2021 年至当前年份；选择年份后下载对应盘点明细 |
| 资产转移单明细查询 | 单据号、调出部门 | `transfer_no`、`out_dept` | 可单独填写单据号、选择调出部门，也可组合筛选 |

## 6. 表字段中英文对应关系

### 6.1 资产处置申请单明细（`EAMPROD.u5_fu_zpczsqdmx01`）

| 中文名称       | 英文字段         | 备注        |
|---------------|-----------------|------------|
| 申请单号       | `apply_no`      |            |
| 处置类型       | `dispose_type`  |            |
| 处置单描述     | `dispose_desc`  |            |
| 使用部门       | `use_dept`      |            |
| 部门描述       | `dept_desc`     |            |
| 创建人         | `creator`       |            |
| 创建日期       | `create_time`   |            |
| 状态           | `status`        |            |
| 状态描述       | `status_desc`   |            |
| 提报日期       | `report_time`   |            |
| 确认日期       | `confirm_time`  |            |
| 处置原因       | `dispose_reason` |            |
| 资产编码       | `asset_code`    |            |
| 资产名称       | `asset_name`    |            |
| 固定资产编码   | `fixed_code`    |            |
| 维修部门       | `maint_dept`    |            |
| 单位           | `unit`          |            |
| 规格型号       | `model`         |            |
| 供应商         | `supplier`      |            |
| 品牌           | `brand`         |            |
| 资产来源       | `source`        |            |
| 设备价值（元） | `equip_value`   |            |
| 分摊价值（元） | `alloc_value`   |            |
| 资产价值（元） | `asset_value`   |            |
| 投运日期       | `operate_date`  |            |
| 验收移交时间   | `handover_time` |            |
| 备注           | `remark`        |            |

### 6.2 资产盘点单明细（`EAMPROD.u5_fu_zppdqd01`）

| 中文名称         | 英文字段          | 备注        |
|-----------------|------------------|------------|
| 盘点单号         | `check_no`        |            |
| 盘点年份         | `check_year`      |            |
| 盘点类型         | `check_type`      |            |
| 盘点单描述       | `check_desc`      |            |
| 部门             | `dept`            |            |
| 部门描述         | `dept_desc`       |            |
| 创建日期         | `create_time`     |            |
| 状态             | `status`          |            |
| 状态描述         | `status_desc`     |            |
| 盘点单创建人     | `check_creator`   |            |
| 盘点清单行创建人 | `line_creator`    |            |
| 盘点行更新人     | `line_updater`    |            |
| 创建人描述       | `creator_desc`    |            |
| 资产编码         | `asset_code`      |            |
| 资产描述         | `asset_desc`      |            |
| 规格型号         | `model`           |            |
| 存放位置         | `store_loc`       |            |
| 安装位置         | `install_loc`     |            |
| 设备价值（元）   | `equip_value`     |            |
| 分摊价值（元）   | `alloc_value`     |            |
| 资产价值（元）   | `asset_value`     |            |
| 固定资产编码     | `fixed_code`      |            |
| 盘点状态         | `check_status`    |            |
| 盘点方式         | `check_method`    |            |

### 6.3 资产转移单明细（`EAMPROD.u5_fu_zpzyqdmx01`）

| 中文名称         | 英文字段          | 备注             |
|-----------------|------------------|-----------------|
| 调拨单号         | `transfer_no`     |                 |
| 转出部门         | `out_dept`        |                 |
| 转入部门         | `in_dept`         |                 |
| 创建日期         | `create_time`     |                 |
| 状态             | `status`          |                 |
| 状态描述         | `status_desc`     |                 |
| 创建人           | `check_creator`   | 原字段名“盘点单创建人”，中文显示名调整为“创建人” |
| 单据描述         | `transfer_desc`   | 原“资产转移单状态”字段，已更名为“单据描述” |
| 设备编码         | `equip_code`      |                 |
| 设备描述         | `equip_desc`      |                 |
| 设备大类         | `equip_type`      |                 |
| 设备分类描述     | `equip_type_desc` |                 |
| 使用部门         | `use_dept`        |                 |
| 规格型号         | `model`           |                 |
| 安装位置         | `install_loc`     |                 |
| 安装位置描述     | `install_desc`    |                 |
| 存放位置         | `store_loc`       |                 |
| 固定资产编码     | `fixed_code`      |                 |
| 单位             | `unit`            |                 |
| 品牌             | `brand`           |                 |
| 供应商           | `supplier`        |                 |
| 设备价值（元）   | `equip_value`     |                 |
| 资产价值（元）   | `asset_value`     |                 |
| 分摊价值（元）   | `alloc_value`     |                 |
| 生产厂家         | `maker`           |                 |
| 投运日期         | `operate_date`    |                 |
| 出厂编号         | `serial_no`       |                 |
| 资产来源         | `source`          |                 |
| 科室             | `office`          |                 |
| 使用人           | `user_name`       |                 |
| 验收移交时间     | `handover_time`   |                 |
| 虚拟设备         | `virtual_flag`    |                 |
| 父设备           | `parent_code`     |                 |
| 备注             | `remark`          |                 |
| 详细说明         | `detail`          |                 |
| 调拨批准时间     | `approve_time`    |                 |

---

## 7. 变更记录

| 日期       | 变更内容 | 说明                                  |
|-----------|---------|---------------------------------------|
| 2026-07-15 | 配置与字段调整 | 三张资产明细视图改用 `EAMPROD` Schema 完整名称；资产处置按部门描述筛选，资产盘点按年份筛选，资产转移按单据号和调出部门筛选；资产转移单补充 `status`、`status_desc`，`check_creator` 中文显示名由“盘点单创建人”调整为“创建人”，原“资产转移单状态”更名为“单据描述”，对应数据库字段 `transfer_desc`。 |

---

*文档版本：v1.0 / 2026-07-15*
