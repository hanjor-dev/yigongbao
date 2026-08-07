package com.yigongbao.module.production.product.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionProductMapperSqlTest {

    private static final Pattern EXPLICIT_ALIAS =
            Pattern.compile("\\bAS\\s+([a-z][a-z0-9_]*)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOINED_TABLE =
            Pattern.compile("\\b(?:INNER|LEFT|RIGHT|FULL|CROSS)?\\s*JOIN\\s+([a-z][a-z0-9_]*)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final String EXPORT_STATUS_PREDICATE =
            "pp.status IN ('in_process', 'fail', 'pass', 'pending_warehouse_in', 'warehoused', 'warehouse_out', 'completed', 'cancelled')";
    private static final List<String> LEDGER_KEYS = List.of(
            "order_code",
            "order_create_time",
            "product_no",
            "file_name",
            "product_name",
            "spec_name",
            "color_name",
            "material_name",
            "print_duration_seconds",
            "weight",
            "processing_duration_seconds",
            "hospital_name",
            "patient_name",
            "patient_gender",
            "patient_age",
            "producer_name",
            "doctor_name",
            "hospital_dept_name",
            "business_operator",
            "warehouse_out_time");
    private static final List<String> DIRECT_LEDGER_PROJECTIONS = List.of(
            "om.order_code AS order_code",
            "om.create_time AS order_create_time",
            "pp.product_no AS product_no",
            "pp.file_name AS file_name",
            "pp.product_name AS product_name",
            "pp.spec_name AS spec_name",
            "pp.color_name AS color_name",
            "pp.material_name AS material_name",
            "pp.weight AS weight",
            "om.hospital_name AS hospital_name",
            "om.patient_name AS patient_name",
            "om.patient_gender AS patient_gender",
            "om.patient_age AS patient_age",
            "pr.producer_name AS producer_name",
            "om.doctor_name AS doctor_name",
            "om.hospital_dept_name AS hospital_dept_name",
            "om.operator_name AS business_operator",
            "pp.warehouse_out_time AS warehouse_out_time");

    @Test
    void exportQueriesUseOrderMainForJoinTimeRangeAndPermissions() {
        var exportQueries = exportQueries();

        assertTrue(exportQueries.size() == 2, "expected both export queries to be present");
        for (Map.Entry<String, String> entry : exportQueries.entrySet()) {
            String query = entry.getValue();
            assertTrue(query.contains("INNER JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0"),
                    () -> entry.getKey() + " must require a non-deleted order");
            assertTrue(query.contains("AND om.create_time &gt;= #{dto.startTime}"),
                    () -> entry.getKey() + " must apply startTime to the order creation time");
            assertTrue(query.contains("AND om.create_time &lt;= #{dto.endTime}"),
                    () -> entry.getKey() + " must apply endTime to the order creation time");
            assertFalse(query.contains("pp.create_time &gt;="),
                    () -> entry.getKey() + " must not apply startTime to the product creation time");
            assertFalse(query.contains("pp.create_time &lt;="),
                    () -> entry.getKey() + " must not apply endTime to the product creation time");
            assertTrue(query.contains(EXPORT_STATUS_PREDICATE),
                    () -> "export query does not use the complete post-print status predicate: " + query);
            assertTrue(query.contains("om.hospital_id IN"),
                    () -> entry.getKey() + " must filter by the order hospital");
            assertTrue(query.contains("om.center_id IN"),
                    () -> entry.getKey() + " must filter by the order processing center");
            assertTrue(query.contains("WHERE pp.is_deleted = 0 AND pr.is_deleted = 0"),
                    () -> entry.getKey() + " must preserve product and production-record logical deletion filters");
        }
    }

    @Test
    void detailQueryProjectsTheCompleteMapperToBuilderKeyContract() {
        String detailQuery = exportQueries().get("listProductLedgerData");
        List<String> projections = mainSelectProjections(detailQuery);
        List<String> aliases = new ArrayList<>();

        for (String projection : projections) {
            Matcher aliasMatcher = EXPLICIT_ALIAS.matcher(projection);
            assertTrue(aliasMatcher.find(),
                    () -> "every main SELECT projection must end with an explicit AS alias: " + projection);
            aliases.add(aliasMatcher.group(1));
        }
        assertEquals(LEDGER_KEYS.size(), projections.size(),
                "detail query must contain exactly the 20 Mapper-to-Builder projections");
        assertEquals(LEDGER_KEYS.size(), aliases.size(),
                "detail query must explicitly alias every Mapper-to-Builder projection");
        assertEquals(Set.copyOf(LEDGER_KEYS), Set.copyOf(aliases),
                "detail query aliases must exactly match the Mapper-to-Builder key contract");

        for (String projection : DIRECT_LEDGER_PROJECTIONS) {
            assertTrue(detailQuery.contains(projection),
                    () -> "detail query uses the wrong source for projection: " + projection);
        }
    }

    @Test
    void detailQueryUsesOnlyRequiredMainJoinsAndCorrelatedProcessSubquery() {
        String detailQuery = exportQueries().get("listProductLedgerData");
        String mainFromSegment = mainFromSegment(detailQuery);
        Matcher joinMatcher = JOINED_TABLE.matcher(mainFromSegment);
        List<String> joinedTables = new ArrayList<>();
        while (joinMatcher.find()) {
            joinedTables.add(joinMatcher.group(1).toLowerCase());
        }

        assertEquals(List.of("production_record", "order_main"), joinedTables,
                "main query must have exactly the two one-product-one-row joins");
        assertTrue(mainFromSegment.contains(
                "INNER JOIN production_record pr ON pp.production_record_id = pr.id"));
        assertTrue(mainFromSegment.contains(
                "INNER JOIN order_main om ON pr.order_id = om.id AND om.is_deleted = 0"));

        String processingDurationProjection = mainSelectProjections(detailQuery).stream()
                .filter(projection -> projection.endsWith("AS processing_duration_seconds"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("processing duration projection is missing"));
        assertTrue(processingDurationProjection.startsWith("(SELECT CASE"),
                "processing duration must use a correlated scalar subquery");
        assertEquals(1, countOccurrences(detailQuery, "production_process"),
                "production_process must appear only once in the detail SQL");
        assertEquals(1, countOccurrences(processingDurationProjection, "production_process"),
                "production_process must appear only inside the processing duration scalar subquery");
    }

    @Test
    void detailQueryCalculatesDurationsWithInvalidDataProtection() {
        String detailQuery = exportQueries().get("listProductLedgerData");

        assertTrue(detailQuery.contains("pr.print_start_time IS NOT NULL"));
        assertTrue(detailQuery.contains("pr.print_finish_time IS NOT NULL"));
        assertTrue(detailQuery.contains("pr.print_finish_time &gt;= pr.print_start_time"));
        assertTrue(detailQuery.contains(
                "TIMESTAMPDIFF(SECOND, pr.print_start_time, pr.print_finish_time)"));
        assertTrue(detailQuery.contains("ELSE NULL END AS print_duration_seconds"));

        assertTrue(detailQuery.contains("FROM production_process p_process"));
        assertTrue(detailQuery.contains("p_process.production_record_id = pr.id"));
        assertTrue(detailQuery.contains("p_process.process_type IN ('wash', 'cure', 'clean_dry')"));
        assertTrue(detailQuery.contains("p_process.is_deleted = 0"));
        assertTrue(detailQuery.contains("COUNT(*) = 0 THEN 0"));
        assertTrue(detailQuery.contains("p_process.start_time IS NULL"));
        assertTrue(detailQuery.contains("p_process.end_time IS NULL"));
        assertTrue(detailQuery.contains("p_process.end_time &lt; p_process.start_time"));
        assertTrue(detailQuery.contains("SUM(TIMESTAMPDIFF(SECOND, p_process.start_time, p_process.end_time))"));
        assertTrue(detailQuery.contains("AS processing_duration_seconds"));
    }

    @Test
    void detailQueryExcludesLegacyProjectionAndUsesStableOrderAndLimit() {
        String detailQuery = exportQueries().get("listProductLedgerData");

        assertFalse(detailQuery.contains("current_process_type"));
        assertFalse(detailQuery.contains("processing_center_name"));
        assertFalse(detailQuery.contains("print_operator"));
        assertFalse(detailQuery.contains("wash_operator"));
        assertFalse(detailQuery.contains("cure_operator"));
        for (String legacyProjection : List.of(
                "pp.cert_no",
                "pp.udi_code",
                "AS product_status",
                "pr.current_process",
                "pp.qc_result",
                "pp.qc_time",
                "pp.qc_remark",
                "pp.warehouse_in_time",
                "pp.create_time",
                "pr.order_type",
                "pr.hospital_name",
                "pr.hospital_dept_name",
                "pr.doctor_name",
                "pr.patient_name",
                "pr.is_urgent",
                "pr.is_postal",
                "pr.expected_delivery_date",
                "pr.record_no,",
                "pr.production_batch_no",
                "pr.design_package_code",
                "pr.print_device_code",
                "pr.material_batch_no",
                "pr.pack_operator_name",
                "pr.pack_time")) {
            assertFalse(detailQuery.contains(legacyProjection),
                    () -> "detail query must not retain legacy projection: " + legacyProjection);
        }
        assertTrue(detailQuery.contains("ORDER BY om.create_time DESC, pp.id DESC"));
        assertTrue(detailQuery.contains("LIMIT 10000"));
    }

    private List<String> mainSelectProjections(String query) {
        int selectStart = query.indexOf("SELECT ");
        int mainFromStart = query.indexOf("FROM production_product pp");
        assertTrue(selectStart >= 0 && mainFromStart > selectStart, "detail query main SELECT is malformed");

        String projectionSegment = query.substring(selectStart + "SELECT ".length(), mainFromStart);
        List<String> projections = new ArrayList<>();
        int parenthesisDepth = 0;
        int projectionStart = 0;
        for (int index = 0; index < projectionSegment.length(); index++) {
            char current = projectionSegment.charAt(index);
            if (current == '(') {
                parenthesisDepth++;
            } else if (current == ')') {
                parenthesisDepth--;
            } else if (current == ',' && parenthesisDepth == 0) {
                projections.add(projectionSegment.substring(projectionStart, index).trim());
                projectionStart = index + 1;
            }
        }
        assertEquals(0, parenthesisDepth, "detail query projection parentheses must be balanced");
        projections.add(projectionSegment.substring(projectionStart).trim());
        return projections;
    }

    private String mainFromSegment(String query) {
        int mainFromStart = query.indexOf("FROM production_product pp");
        int whereStart = query.indexOf("WHERE pp.is_deleted = 0", mainFromStart);
        assertTrue(mainFromStart >= 0 && whereStart > mainFromStart, "detail query main FROM is malformed");
        return query.substring(mainFromStart, whereStart);
    }

    private int countOccurrences(String value, String target) {
        int count = 0;
        int searchFrom = 0;
        while ((searchFrom = value.indexOf(target, searchFrom)) >= 0) {
            count++;
            searchFrom += target.length();
        }
        return count;
    }

    private Map<String, String> exportQueries() {
        return Arrays.stream(ProductionProductMapper.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("listProductLedgerData")
                        || method.getName().equals("countProductLedgerData"))
                .collect(java.util.stream.Collectors.toMap(
                        java.lang.reflect.Method::getName,
                        method -> String.join(" ", method.getAnnotation(Select.class).value())));
    }
}
