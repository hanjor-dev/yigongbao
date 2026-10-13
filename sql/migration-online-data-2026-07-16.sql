-- ============================================================
-- Online data migration for schema migration 2026-07-16
-- Run after: sql/migration-online-schema-2026-07-16.sql
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 0. order cancel apply resources and role permissions
-- Resource rows are resolved by resource_code so this remains safe
-- when online auto-increment values differ from init.sql.
-- ============================================================

SELECT '0. order cancel apply resources and role permissions' AS step;

SET @order_resource_parent_id = (
    SELECT id FROM sys_resource
    WHERE resource_code = 'Order' AND is_deleted = 0
    LIMIT 1
);

INSERT INTO sys_resource (
    parent_id, resource_name, resource_code, resource_type,
    icon, path, component, redirect, sort, visible, status, remark,
    create_time, update_time, create_by, update_by, is_deleted
)
SELECT @order_resource_parent_id, '我的申请Tab', 'order:TabMyCancel', 3,
       NULL, NULL, NULL, NULL, 23, 1, 1, NULL,
       NOW(), NOW(), NULL, NULL, 0
WHERE @order_resource_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE resource_code = 'order:TabMyCancel' AND is_deleted = 0
  );

INSERT INTO sys_resource (
    parent_id, resource_name, resource_code, resource_type,
    icon, path, component, redirect, sort, visible, status, remark,
    create_time, update_time, create_by, update_by, is_deleted
)
SELECT @order_resource_parent_id, '待审核申请列表', 'order:CancelApply', 3,
       NULL, NULL, NULL, NULL, 24, 1, 1, NULL,
       NOW(), NOW(), NULL, NULL, 0
WHERE @order_resource_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE resource_code = 'order:CancelApply' AND is_deleted = 0
  );

INSERT INTO sys_resource (
    parent_id, resource_name, resource_code, resource_type,
    icon, path, component, redirect, sort, visible, status, remark,
    create_time, update_time, create_by, update_by, is_deleted
)
SELECT @order_resource_parent_id, '审核通过', 'order:CancelApprove', 3,
       NULL, NULL, NULL, NULL, 25, 1, 1, NULL,
       NOW(), NOW(), NULL, NULL, 0
WHERE @order_resource_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE resource_code = 'order:CancelApprove' AND is_deleted = 0
  );

INSERT INTO sys_resource (
    parent_id, resource_name, resource_code, resource_type,
    icon, path, component, redirect, sort, visible, status, remark,
    create_time, update_time, create_by, update_by, is_deleted
)
SELECT @order_resource_parent_id, '审核驳回', 'order:CancelReject', 3,
       NULL, NULL, NULL, NULL, 26, 1, 1, NULL,
       NOW(), NOW(), NULL, NULL, 0
WHERE @order_resource_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE resource_code = 'order:CancelReject' AND is_deleted = 0
  );

INSERT INTO sys_resource (
    parent_id, resource_name, resource_code, resource_type,
    icon, path, component, redirect, sort, visible, status, remark,
    create_time, update_time, create_by, update_by, is_deleted
)
SELECT @order_resource_parent_id, '取消申请历史', 'order:CancelHistory', 3,
       NULL, NULL, NULL, NULL, 27, 1, 1, NULL,
       NOW(), NOW(), NULL, NULL, 0
WHERE @order_resource_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE resource_code = 'order:CancelHistory' AND is_deleted = 0
  );

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM sys_role r
JOIN sys_resource res ON res.resource_code = 'order:TabMyCancel' AND res.is_deleted = 0
WHERE r.role_code IN ('admin', 'salesman', 'salesman-self')
  AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource rr
      WHERE rr.role_id = r.id AND rr.resource_id = res.id
  );

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM sys_role r
JOIN sys_resource res ON res.resource_code = 'order:Cancel' AND res.is_deleted = 0
WHERE r.role_code = 'salesman-self'
  AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource rr
      WHERE rr.role_id = r.id AND rr.resource_id = res.id
  );

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM sys_role r
JOIN sys_resource res ON res.resource_code = 'design:Cancel' AND res.is_deleted = 0
WHERE r.role_code = 'designer'
  AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource rr
      WHERE rr.role_id = r.id AND rr.resource_id = res.id
  );

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM sys_role r
JOIN sys_resource res
  ON res.resource_code IN (
      'order:CancelApply', 'order:CancelApprove',
      'order:CancelReject', 'order:CancelHistory'
  )
 AND res.is_deleted = 0
WHERE r.role_code = 'designer-manager'
  AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource rr
      WHERE rr.role_id = r.id AND rr.resource_id = res.id
  );

-- ============================================================
-- 1. Service provider data migration from migration_20260627_service_provider.sql
-- ============================================================

SELECT '1. service provider dictionary and role data' AS step;

SET @org_type_parent_id = (
    SELECT id
    FROM sys_dict
    WHERE dict_code = '1'
      AND is_deleted = 0
    LIMIT 1
);

INSERT INTO sys_dict (dict_code, dict_name, parent_id, sort, status, remark, create_time, update_time, is_deleted)
SELECT '1.4', '服务商', @org_type_parent_id, 4, 1, '企业自营业务机构', NOW(), NOW(), 0
WHERE @org_type_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_dict WHERE dict_code = '1.4' AND is_deleted = 0
  );

INSERT INTO sys_role (role_name, role_code, account_type, data_scope_type, status, remark, create_time, update_time, is_deleted)
SELECT '业务员（自营）', 'salesman-self', '6.1', 'hospitals', 1, '企业自营业务人员，负责自营订单创建和跟进', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'salesman-self' AND is_deleted = 0
);

UPDATE sys_role
SET data_scope_type = 'hospitals'
WHERE role_code = 'salesman-self'
  AND is_deleted = 0
  AND data_scope_type = 'HOSPITALS';

SET @salesman_role_id = (
    SELECT id FROM sys_role WHERE role_code = 'salesman' AND is_deleted = 0 LIMIT 1
);
SET @salesman_self_role_id = (
    SELECT id FROM sys_role WHERE role_code = 'salesman-self' AND is_deleted = 0 LIMIT 1
);

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT @salesman_self_role_id, srr.resource_id
FROM sys_role_resource srr
WHERE srr.role_id = @salesman_role_id
  AND @salesman_role_id IS NOT NULL
  AND @salesman_self_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_resource existed
      WHERE existed.role_id = @salesman_self_role_id
        AND existed.resource_id = srr.resource_id
  );

SET @manufacturer_org_id = (
    SELECT config_value
    FROM sys_config
    WHERE config_key = 'manufacturer.org.id'
      AND is_deleted = 0
    LIMIT 1
);
SET @manufacturer_org_id = IFNULL(@manufacturer_org_id, 1);

INSERT INTO sys_dept_org (dept_id, org_id, create_time)
SELECT d.id, @manufacturer_org_id, NOW()
FROM sys_dept d
WHERE d.dept_type = '6.1'
  AND d.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_dept_org dorg
      WHERE dorg.dept_id = d.id
  );

-- ============================================================
-- 2. sys_resource historical resource_code case fix
-- ============================================================

SELECT '2. fix sys_resource order review resource_code case' AS step;

UPDATE sys_resource
SET resource_code = 'order:Review',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1115
  AND resource_code = 'order:review'
  AND is_deleted = 0;

-- ============================================================
-- 3. order_main.has_pending_cancel_apply backfill
-- ============================================================

SELECT '3. order pending cancel marker' AS step;

UPDATE order_main
SET has_pending_cancel_apply = 0
WHERE has_pending_cancel_apply IS NULL;

UPDATE order_main om
SET om.has_pending_cancel_apply = CASE
    WHEN EXISTS (
        SELECT 1
        FROM order_cancel_apply oca
        WHERE oca.order_id = om.id
          AND oca.audit_status = 1
          AND oca.is_deleted = 0
    ) THEN 1
    ELSE 0
END;

-- ============================================================
-- 4. device_daily_usage_counter seed from existing product numbers
-- Product number format:
--   YYMMDD + product code(1) + device no(3) + usage count(3) + sequence(2)
-- ============================================================

SELECT '4. seed device daily usage counter' AS step;

INSERT INTO device_daily_usage_counter (
    device_id,
    usage_date,
    usage_count,
    version,
    create_time,
    update_time,
    is_deleted
)
SELECT
    d.id AS device_id,
    STR_TO_DATE(CONCAT('20', SUBSTRING(pp.product_no, 1, 6)), '%Y%m%d') AS usage_date,
    MAX(CAST(SUBSTRING(pp.product_no, 11, 3) AS UNSIGNED)) AS usage_count,
    0 AS version,
    NOW() AS create_time,
    NOW() AS update_time,
    0 AS is_deleted
FROM production_product pp
INNER JOIN device d
    ON LPAD(
        CASE
            WHEN LOCATE('-', d.device_id) > 0 THEN SUBSTRING_INDEX(d.device_id, '-', -1)
            ELSE d.device_id
        END,
        3,
        '0'
    ) = SUBSTRING(pp.product_no, 8, 3)
WHERE pp.is_deleted = 0
  AND pp.product_no IS NOT NULL
  AND LENGTH(pp.product_no) >= 15
  AND SUBSTRING(pp.product_no, 1, 6) REGEXP '^[0-9]{6}$'
  AND SUBSTRING(pp.product_no, 8, 3) REGEXP '^[0-9]{3}$'
  AND SUBSTRING(pp.product_no, 11, 3) REGEXP '^[0-9]{3}$'
GROUP BY d.id, STR_TO_DATE(CONCAT('20', SUBSTRING(pp.product_no, 1, 6)), '%Y%m%d')
ON DUPLICATE KEY UPDATE
    usage_count = GREATEST(device_daily_usage_counter.usage_count, VALUES(usage_count)),
    update_time = NOW();

-- ============================================================
-- 5. production_record product/category backfill
-- Conservative rule:
--   only one active production_record under a design_package
--   and the design_package maps to exactly one product_id.
-- Multi-product old packages are reported for manual handling.
-- ============================================================

SELECT '5. backfill production_record product fields' AS step;

UPDATE production_record pr
INNER JOIN (
    SELECT
        r.design_package_id,
        COUNT(*) AS active_record_count
    FROM production_record r
    WHERE r.is_deleted = 0
    GROUP BY r.design_package_id
    HAVING COUNT(*) = 1
) single_record_pkg ON single_record_pkg.design_package_id = pr.design_package_id
INNER JOIN (
    SELECT
        dp.package_id,
        MIN(dp.product_id) AS product_id,
        MAX(dp.product_name) AS product_name,
        COUNT(DISTINCT dp.product_id) AS product_count
    FROM design_product dp
    WHERE dp.is_deleted = 0
      AND dp.product_id IS NOT NULL
    GROUP BY dp.package_id
    HAVING COUNT(DISTINCT dp.product_id) = 1
) single_product_pkg ON single_product_pkg.package_id = pr.design_package_id
LEFT JOIN product p ON p.id = single_product_pkg.product_id AND p.is_deleted = 0
SET
    pr.product_id = single_product_pkg.product_id,
    pr.product_name = COALESCE(
        pr.product_name COLLATE utf8mb4_unicode_ci,
        single_product_pkg.product_name COLLATE utf8mb4_unicode_ci,
        p.product_name COLLATE utf8mb4_unicode_ci
    ),
    pr.product_category = COALESCE(
        pr.product_category COLLATE utf8mb4_unicode_ci,
        p.category COLLATE utf8mb4_unicode_ci
    ),
    pr.product_category_name = COALESCE(
        pr.product_category_name COLLATE utf8mb4_unicode_ci,
        p.category_name COLLATE utf8mb4_unicode_ci
    ),
    pr.update_time = CURRENT_TIMESTAMP
WHERE pr.is_deleted = 0
  AND pr.product_id IS NULL;

-- Secondary rule:
--   if production products under a record have exactly one product_name,
--   and product master has exactly one active product with that name,
--   fill product_id/category when it will not violate uk_package_product.
UPDATE production_record pr
INNER JOIN (
    SELECT
        r.design_package_id,
        COUNT(*) AS active_record_count
    FROM production_record r
    WHERE r.is_deleted = 0
    GROUP BY r.design_package_id
    HAVING COUNT(*) = 1
) single_record_pkg ON single_record_pkg.design_package_id = pr.design_package_id
INNER JOIN (
    SELECT
        pp.production_record_id,
        MAX(pp.product_name) AS product_name,
        COUNT(DISTINCT pp.product_name) AS product_name_count
    FROM production_product pp
    WHERE pp.is_deleted = 0
      AND pp.product_name IS NOT NULL
    GROUP BY pp.production_record_id
    HAVING COUNT(DISTINCT pp.product_name) = 1
) one_name ON one_name.production_record_id = pr.id
INNER JOIN (
    SELECT
        product_name,
        MIN(id) AS product_id,
        COUNT(*) AS product_master_count
    FROM product
    WHERE is_deleted = 0
    GROUP BY product_name
    HAVING COUNT(*) = 1
) one_product ON one_product.product_name COLLATE utf8mb4_unicode_ci
    = one_name.product_name COLLATE utf8mb4_unicode_ci
LEFT JOIN product p ON p.id = one_product.product_id AND p.is_deleted = 0
SET
    pr.product_id = one_product.product_id,
    pr.product_name = COALESCE(
        pr.product_name COLLATE utf8mb4_unicode_ci,
        one_name.product_name COLLATE utf8mb4_unicode_ci,
        p.product_name COLLATE utf8mb4_unicode_ci
    ),
    pr.product_category = COALESCE(
        pr.product_category COLLATE utf8mb4_unicode_ci,
        p.category COLLATE utf8mb4_unicode_ci
    ),
    pr.product_category_name = COALESCE(
        pr.product_category_name COLLATE utf8mb4_unicode_ci,
        p.category_name COLLATE utf8mb4_unicode_ci
    ),
    pr.update_time = CURRENT_TIMESTAMP
WHERE pr.is_deleted = 0
  AND pr.product_id IS NULL;

-- Fill category for records that already have product_id.
UPDATE production_record pr
INNER JOIN product p ON p.id = pr.product_id AND p.is_deleted = 0
SET
    pr.product_name = COALESCE(
        pr.product_name COLLATE utf8mb4_unicode_ci,
        p.product_name COLLATE utf8mb4_unicode_ci
    ),
    pr.product_category = COALESCE(
        pr.product_category COLLATE utf8mb4_unicode_ci,
        p.category COLLATE utf8mb4_unicode_ci
    ),
    pr.product_category_name = COALESCE(
        pr.product_category_name COLLATE utf8mb4_unicode_ci,
        p.category_name COLLATE utf8mb4_unicode_ci
    ),
    pr.update_time = CURRENT_TIMESTAMP
WHERE pr.is_deleted = 0
  AND pr.product_id IS NOT NULL
  AND (
      pr.product_category IS NULL
      OR pr.product_category_name IS NULL
      OR pr.product_name IS NULL
  );

-- ============================================================
-- 6. Update regional administrator data scope
-- ============================================================

UPDATE sys_role
SET data_scope_type = 'dept'
WHERE id = 3
  AND role_name = '区域管理员'
  AND data_scope_type = 'self';

-- ============================================================
-- 7. 图纸二维码文件业务字典
-- The QR image itself is uploaded after deployment. Existing drawings
-- intentionally keep qr_file_id NULL because the old backend-generated
-- QR bytes were not persisted as reusable files.
-- ============================================================

SELECT '7. drawing QR image dictionary seed' AS step;

SET @file_biz_type_parent_id = (
    SELECT id
    FROM sys_dict
    WHERE dict_code = '10'
      AND is_deleted = 0
    LIMIT 1
);

INSERT INTO sys_dict (
    dict_code, dict_name, parent_id, dict_value, level, sort, status,
    remark, create_time, update_time, is_deleted
)
SELECT
    '10.21', '图纸二维码图片', @file_biz_type_parent_id, NULL, 2, 21, 1,
    '前端生成的二维码PNG，按订单关联并用于图纸生成', NOW(), NOW(), 0
WHERE @file_biz_type_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_dict
      WHERE dict_code = '10.21'
        AND is_deleted = 0
  );

SELECT
    id, parent_id, dict_code, dict_name, dict_value, level, sort, status
FROM sys_dict
WHERE dict_code = '10.21'
  AND is_deleted = 0;

-- ============================================================
-- 8. Verification and manual follow-up reports
-- ============================================================

SELECT '8. verification summary' AS step;

SELECT
    COUNT(*) AS total_active_records,
    SUM(CASE WHEN product_id IS NULL THEN 1 ELSE 0 END) AS records_without_product_id,
    SUM(CASE WHEN product_category IS NULL THEN 1 ELSE 0 END) AS records_without_category,
    SUM(CASE WHEN product_category_name IS NULL THEN 1 ELSE 0 END) AS records_without_category_name
FROM production_record
WHERE is_deleted = 0;

SELECT
    pr.id,
    pr.record_no,
    pr.design_package_id,
    pr.design_package_code,
    pr.product_id,
    pr.product_name,
    COUNT(DISTINCT dp.product_id) AS design_package_product_count,
    GROUP_CONCAT(
        DISTINCT CONCAT(
            dp.product_id,
            ':',
            dp.product_name COLLATE utf8mb4_unicode_ci
        )
        ORDER BY dp.product_id
        SEPARATOR ', '
    ) AS candidate_products,
    'manual_check_required: multiple or missing design products' AS reason
FROM production_record pr
LEFT JOIN design_product dp
    ON dp.package_id = pr.design_package_id
   AND dp.is_deleted = 0
WHERE pr.is_deleted = 0
  AND pr.product_id IS NULL
GROUP BY pr.id, pr.record_no, pr.design_package_id, pr.design_package_code, pr.product_id, pr.product_name
ORDER BY pr.id;

SELECT
    device_id,
    usage_date,
    usage_count
FROM device_daily_usage_counter
WHERE is_deleted = 0
ORDER BY usage_date DESC, device_id
LIMIT 50;

SELECT 'data migration complete' AS status, NOW() AS finished_at;
