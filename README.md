# 企业资产图表分析 — Web 服务端

为「企业资产图表分析」单页应用提供 HTTP 服务，支持 **Oracle 12c**（生产）和 **SQLite**（测试）两种数据库。

## 项目结构

```
├── deps.edn                    # Clojure 依赖
├── config.edn                  # Oracle 生产配置
├── config-sqlite.edn           # SQLite 测试配置
├── sql/
│   └── oracle-init.sql         # Oracle 建表 DDL（10 张表）
├── src/ocvate/
│   ├── core.clj                # 入口点
│   ├── config.clj              # 配置加载
│   ├── db.clj                  # 连接池 + 查询
│   └── server.clj              # Web 路由
├── resources/public/
│   └── index.html              # 前端单页应用
├── deploy/                     # 部署脚本
│   ├── build.bat               # Windows 构建
│   ├── build.sh                # macOS/Linux 构建
│   ├── run.bat                 # Windows Oracle 启动
│   └── run-sqlite.bat          # Windows SQLite 启动
└── target/
    └── ocvate.jar              # 构建产物（uber JAR）
```

## Windows 部署

### 前置条件
- Java 17+（GraalVM 或 OpenJDK 均可）
- Clojure CLI（用于构建）

### 1. 构建 JAR
```cmd
deploy\build.bat
```
产出 `target\ocvate.jar`（约 30MB，含全部依赖）。

### 2. SQLite 测试模式（无需数据库）
```cmd
deploy\run-sqlite.bat
```
自动建表并插入测试数据，访问 http://127.0.0.1:8080

### 3. Oracle 生产模式
```cmd
deploy\run.bat
```
读取 `config.edn` 连接 Oracle 数据库。

## Oracle 配置

编辑 `config.edn`：

```clojure
{:db {:dbtype "oracle"
      :dbname "XE"            ;; SID 或 Service Name
      :host  "127.0.0.1"
      :port  1521
      :user  "scott"
      :password "tiger"}
 :server {:port 8080}}
```

### Oracle 建表
```sql
sqlplus scott/tiger@XE @sql/oracle-init.sql
```

## Oracle 视图/表结构

`sql/oracle-init.sql` 包含 10 张表的完整 DDL，与前端 `state.data` 一一对应：

| 表名 | 前端字段 | 说明 |
|------|---------|------|
| `dept_rank` | `departments` | 部门资产统计 |
| `asset_type` | `assetTypes` | 资产类型统计 |
| `depreciation` | `depreciation` | 折旧到期统计 |
| `annual_dynamics` | `annualDynamics` | 年度资产动态 |
| `outbound_summary` | `outboundSummary` | 出库汇总 |
| `outbound_details` | `outboundDetails` | 出库明细 |
| `repairs` | `repairs` | 维修明细 |
| `monthly_fuel` | `monthlyFuel` | 月度油耗 |
| `department_fuel` | `departmentFuel` | 部门油耗 |
| `vehicle_fuel` | `vehicleFuel` | 单车油耗 |

## API

前端自动调用 `GET /api/all` 获取全部数据，数据格式与 `state.data` 完全一致。

## 从源码运行（开发模式）

```bash
clojure -J-Dconf=config-sqlite.edn -M -m ocvate.core
```

## 注意事项

- Oracle JDBC 驱动已内置于 JAR，无需额外下载
- 需要将 Oracle 数据库中的实际表名/字段名与 `sql/queries.sql` 对应
- 生产环境请修改 `config.edn` 中的数据库连接信息
