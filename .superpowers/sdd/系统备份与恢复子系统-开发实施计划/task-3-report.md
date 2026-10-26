# Task 3 实施报告：线上机器身份认证

## 结果

已完成内部机器 HMAC 认证链路，并接入 `MachineHmacAuthenticationFilter`。过滤器只对 context-path 之后的 `/internal/*` 生效；请求体缓存后同时提供给验签器和业务控制器，认证失败统一返回 `VERIFICATION_REJECTED`，不会调用业务链路。

## 安全实现

- 规范请求由 method、context-relative path、严格 percent-decoded 后排序的 query、body SHA-256 及认证头组成；重复 query 名、缺少 `=`、非法 percent/UTF-8 编码均拒绝。
- 写请求要求 `X-Key-Id`、`X-Nonce`、`X-Timestamp`、`X-Signature`、`X-Scope`、`Idempotency-Key`；时间窗口默认 300 秒且拒绝非法、过期、未来时间。
- scope 仅允许 `MANIFEST`、`ACCOUNT`、`RESTORE`、`CLEANUP`、`CLEANUP_PERMIT`，并按 endpoint 强制匹配。
- HMAC-SHA256 使用 `MessageDigest.isEqual` 常量时间比较；key 状态支持 ACTIVE/RETIRING 重叠，REVOKED/过期/未知 key 立即拒绝。
- nonce 只保存 SHA-256，生产登记通过 `backup_hmac_nonce` 的 `(machine_id,key_id,nonce_hash)` 唯一索引原子插入；密钥正文不进 SQL，secret ref 从配置环境解析。
- 新增机器身份、key 元数据、nonce 实体与 MyBatis-Plus 注解 Mapper，无 Mapper XML。审计日志只包含 key ID、机器 ID、scope、结果和 request ID，不包含 URL、body、签名或密钥。

## 验证

- `mvn -pl yigongbao-module-backup -am "-DskipTests" compile`：通过。
- `mvn -pl yigongbao-module-backup "-Dtest=HmacMachineRequestVerifierTest,MachineHmacAuthenticationFilterTest,BackupApplicationContextTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：25 tests，0 failures，0 errors。
- 全量 `mvn -pl yigongbao-module-backup -am test` 在既有 `yigongbao-module-basic` 基线失败（41 failures、11 errors），未修改无关测试或模块；备份模块聚焦测试仍通过。

## 注意事项

生产部署仍需先执行既有 `2026-10-27-04-backup-security.sql` 并配置 `secret_ref` 指向外部密钥存储；未配置 Mapper/数据库时实现保持 fail-closed，不会放行请求。

## 审查修复

- 修复 `key_id` 跨机器重复时由 `LIMIT 1` 随机选取凭证的问题：认证查询现在必须恰好返回一条未删除记录，否则 fail-closed；并补充重复 key 的单元测试。
- 为请求体缓存增加 `yigongbao.backup.security.max-body-bytes` 配置，默认 10 MiB；先检查 Content-Length，再分块读取未知长度请求，超过上限在验签前拒绝，避免无界内存分配。
- 修正内部 OpenAPI 的 HMAC 头说明、认证时间格式和 `Idempotency-Key` 约束，使契约与实现（X-Key-Id、X-Nonce、X-Timestamp、X-Scope、X-Signature，写请求追加幂等键）一致。

审查修复后的聚焦测试：27 tests，0 failures，0 errors；`mvn -pl yigongbao-module-backup -am -DskipTests compile` 通过。
