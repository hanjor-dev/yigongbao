# Schema Diff Plan 2026-07-16

## Goal
Compare local latest schema `ddl.sql` with production schema `ddl-prod.sql`, cross-check alter/migration scripts under `sql/`, then generate:

- an online-runnable schema migration script
- an online-runnable data migration script
- a detailed difference table for review

## Scope
- Primary schema sources: `sql/ddl.sql`, `sql/ddl-prod.sql`
- Supporting scripts: `sql/*alter*.sql`, `sql/*migration*.sql`
- Reference only unless needed: `sql/init.sql`, `sql/sys_area.sql`

## Phases
- [x] Inventory SQL files
- [x] Parse schema differences
- [x] Cross-check supporting migration scripts
- [x] Generate migration SQL files
- [x] Summarize differences and risks

## Notes
- Existing root `task_plan.md` belongs to a different design-module task and is not modified.
- Generated `sql/schema-diff-report-2026-07-16.md`.
- Generated `sql/migration-online-schema-2026-07-16.sql`.
- Generated `sql/migration-online-data-2026-07-16.sql`.
- `idx_production_record_category` is included because it exists in alter scripts even though current `ddl.sql` does not contain it.
- Historical multi-product production records are not automatically split; the data migration script reports them for manual confirmation.
