package com.yigongbao.module.production.product.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionProductMapperSqlTest {

    private static final String EXPORT_STATUS_PREDICATE =
            "pp.status IN ('in_process', 'fail', 'pass', 'pending_warehouse_in', 'warehoused', 'warehouse_out', 'completed', 'cancelled')";

    @Test
    void exportQueriesIncludeEveryPostPrintProductStatus() {
        var exportQueries = exportQueries();

        assertTrue(exportQueries.size() == 2, "expected both export queries to be present");
        for (String query : exportQueries.values()) {
            assertTrue(query.contains(EXPORT_STATUS_PREDICATE),
                    () -> "export query does not use the complete post-print status predicate: " + query);
            assertTrue(!query.contains("pp.status IN ('in_process', 'fail', 'pass', 'completed')"),
                    () -> "export query still uses the old status predicate: " + query);
        }
    }

    @Test
    void exportQueriesUseFlowCardProcessAndOrderCenterSources() {
        String detailQuery = exportQueries().get("listProductLedgerData");
        assertTrue(detailQuery.contains("pr.current_process AS current_process_type"),
                "current process must come from the production flow card");
        assertTrue(detailQuery.contains("COALESCE(pc.center_name, om.center_name) AS processing_center_name"),
                "processing center name must come from the processing center record");
        assertTrue(detailQuery.contains("LEFT JOIN processing_center pc ON om.center_id = pc.id"),
                "export must join the processing center table by the order center id");
        assertFalse(detailQuery.contains("pr.version_no"),
                "version must not be selected for the Excel export");

        for (Map.Entry<String, String> entry : exportQueries().entrySet()) {
            assertTrue(entry.getValue().contains("om.center_id IN"),
                    () -> entry.getKey() + " must filter by the order main processing center");
            assertFalse(entry.getValue().contains("pr.processing_center_id IN"),
                    () -> entry.getKey() + " must not filter by the unused flow-card center field");
        }
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
