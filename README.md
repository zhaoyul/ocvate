# 企业资产图表分析 — Web 服务端

为「企业资产图表分析」单页应用提供 HTTP 服务，通过 Oracle 12c 数据库驱动数据。

## 项目结构

```
ocvate/
├── deps.edn                  # Clojure 依赖管理
├── config.edn                # 数据库 & 服务器配置（按需修改）
├── sql/
│   └── queries.sql           # HugSQL 查询定义
├── src/
│   └── ocvate/
│       ├── core.clj          # 入口点
│       ├── config.clj        # 配置加载
│       ├── db.clj            # 数据库连接池 + HugSQL
│       └── server.clj        # Web 路由与处理器
├── resources/
│   └── public/
│       └── index.html        # 前端单页应用
└── .gitignore
```

## 快速开始

### 1. 修改配置

编辑 `config.edn`，填入实际的 Oracle 数据库连接信息：

```clojure
{:db {:dbtype "oracle"
      :dbname "XE"         ;; 你的 Oracle SID 或 Service Name
      :host  "127.0.0.1"   ;; 数据库主机
      :port  1521
      :user  "scott"       ;; 数据库用户名
      :password "tiger"}   ;; 密码
 :server {:port 8080}}
```

### 2. 修改 SQL 查询

编辑 `sql/queries.sql`，将 `v_assets`、`v_outbound`、`v_repairs`、`v_fuel`、`v_annual_dynamics` 
等视图名替换为你数据库中实际存在的视图/表名，并调整字段名以匹配。

### 3. 运行

```bash
clojure -M -m ocvate.core
```

服务器将在 `http://127.0.0.1:8080` 启动。

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| GET | `/api/assets/by-type` | 按资产类型统计 |
| GET | `/api/assets/by-dept` | 按使用部门统计 |
| GET | `/api/assets/by-year?year=2024` | 按年份查询资产 |
| GET | `/api/depreciation` | 折旧统计 |
| GET | `/api/annual-dynamics` | 年度资产动态 |
| GET | `/api/outbound` | 备件出库统计 |
| GET | `/api/repair` | 维修维护统计 |
| GET | `/api/fuel` | 油耗统计 |
| POST | `/api/custom-sql` | 自定义 SQL 查询 |

## 构建独立 JAR

```bash
clojure -X:uber
```

## 注意事项

- 需要 Java 17+ 和 Clojure CLI 1.12+
- Oracle JDBC 驱动由 deps.edn 自动从 Maven 仓库下载
- 当前前端为独立 HTML，数据通过本地 XLSX 文件导入；后端 API 为后续改造预留
