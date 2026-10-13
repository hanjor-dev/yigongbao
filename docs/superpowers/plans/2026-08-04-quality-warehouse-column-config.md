# Quality and Warehouse Column Configuration Implementation Plan

> **For agentic workers:** Execute this plan task by task. Preserve unrelated working-tree changes.

**Goal:** 仅在后端为质检列表和仓储列表补齐与订单、设计、生产列表一致的用户级列显示配置能力：未保存时读取 `sys_config` 默认 JSON，保存后读取 `sys_user` 个人 JSON；同步数据库迁移和接口文档，不修改前端。

**Architecture:** 沿用生产列表现有的“系统配置 + 用户字段 + GET/POST 配置接口”后端模式，质检和仓储分别拥有独立的系统配置键、用户字段、VO/DTO、Service 方法和 Controller。默认字段定义与现有前端页面保持兼容，但本次不修改前端源码。

**Tech Stack:** Java/Spring Boot/MyBatis-Plus/Jackson/Sa-Token, MySQL 8, Vue 3/TypeScript/Element Plus/SortableJS, Maven and npm.

## Scope and database findings

- Current database is `yigongbao`, MySQL `8.0.45`; `sys_user` has `order_column_settings`, `design_column_settings`, `production_column_settings`, but no quality/warehouse fields.
- Existing active `sys_config` keys are `order.column.config`, `design.column.config`, and `production.column.config`; add `quality.column.config` and `warehouse.column.config`.
- Existing users must not be backfilled with a copied JSON. New columns remain `NULL`, so they transparently use system defaults until the user saves.
- The migration is appended to `sql/migration-drawing-product-category-2026-07-17.sql` and is idempotent. `sql/ddl.sql`, `sql/ddl-prod.sql`, `sql/init.sql`, and the system test schema are kept in sync.
- Do not execute DDL or DML against the live local `yigongbao` schema during implementation; validate migration text and application behavior locally.

## Tasks

### 1. Establish tests before implementation

- Add service tests for quality and warehouse GET fallback (user JSON, no user JSON, malformed user JSON) and SAVE serialization into the corresponding user field.
- Add controller tests for GET/POST delegation and validation of a missing column list.
- Run the focused production-module tests and record the expected compilation/test failure before adding production classes.

Verification:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=ProductionQcColumnConfigControllerTest,WarehouseColumnConfigControllerTest,ProductionQcServiceImplTest,WarehouseServiceImplTest test
```

### 2. Implement backend configuration contracts

- Add `QcColumnConfigVO`/`SaveQcColumnConfigDTO` under `production/qc` and warehouse equivalents under `production/warehouse`, each with `module` and ordered `columns` containing `field`, `label`, `width`, and optional visibility metadata matching the production configuration contract.
- Extend `IProductionQcService` and `IWarehouseService` with `getColumnConfig()` and `saveColumnConfig(...)`.
- Implement user-first/system-fallback JSON handling using `UserService`, `ConfigService`, `ObjectMapper`, and the current authenticated user. A malformed personal JSON falls back to the system default and does not break list access.
- Add `/production/qc/column-config` and `/production/warehouse/column-config` GET/POST endpoints with the same authentication and response conventions as `/production/column-config`.
- Do not add fields to `UserVO`; these are personal UI settings and existing production settings are not exposed there.

Verification:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -Dtest=ProductionQcColumnConfigControllerTest,WarehouseColumnConfigControllerTest,ProductionQcServiceImplTest,WarehouseServiceImplTest test
```

### 3. Add database schema, defaults, and migration

- Add nullable `TEXT` columns `quality_column_settings` and `warehouse_column_settings` to `sys_user`, adjacent to `production_column_settings`, with JSON-purpose comments.
- Add the two default `sys_config` rows to `sql/init.sql` and append an idempotent section to `sql/migration-drawing-product-category-2026-07-17.sql` that adds missing columns, inserts missing active config rows, and performs post-change checks.
- Keep `sql/ddl.sql`, `sql/ddl-prod.sql`, and `yigongbao-parent/yigongbao-module-system/src/test/resources/schema.sql` aligned with the entity schema.
- Default quality columns: `recordNo`, `designPackageCode`, `productionBatchNo`, `orderCode`, `hospitalName`, `hospitalDeptName`, `doctorName`, `patientName`, `isUrgent`, `isPostal`, `expectedDeliveryDate`, `orgName`, `totalProductCount`, `qualifiedCount`, `unqualifiedCount`, `pendingCount`, `status`, `createTime`, and `action` fixed right.
- Default warehouse columns: `recordNo`, `designPackageCode`, `status`, `productionBatchNo`, `orderNo`, `hospitalName`, `hospitalDeptName`, `doctorName`, `patientName`, `isUrgent`, `isPostal`, `expectedDeliveryDate`, `totalCount`, `warehouseCountSummary`, `earliestInTime`, `latestOutTime`, and `action` fixed right.
- Keep the row `index` column outside the configuration. The `action` item is included for compatibility with the existing production configuration contract and is marked `fixed: right`.

Verification:

```powershell
rg -n "quality_column_settings|warehouse_column_settings|quality\.column\.config|warehouse\.column\.config" sql yigongbao-parent
git diff --check
```

### 4. Update interface documentation

- Update `.docs/接口文档/23-2_生产模块接口文档.md` with the quality column-config controller and GET/POST contracts, including request/response examples and default field semantics.
- Update `.docs/接口文档/23_生产模块接口文档.md` as the older maintained production-document duplicate so it does not omit the new `/production/qc/column-config` endpoints.
- Update `.docs/接口文档/25_仓储模块接口文档.md` with the warehouse column-config controller, GET/POST contracts, and `warehouseCountSummary` display semantics.
- Update document version/date and interface counts only where the existing document format requires it; do not renumber unrelated APIs.

### 5. Self-review and verification

- Review the diff for: user-first fallback, malformed JSON fallback, no historical data backfill, idempotent migration, matching default JSON/VO/API fields, endpoint/document consistency, and preservation of unrelated changes. Frontend files must remain unchanged.
- Run focused backend tests, production-module compile/package validation, `git diff --check`, and inspect the backend repository status/diff summary.
- Do not commit or stage unless explicitly requested.

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am test
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -DskipTests package
git diff --check
```
