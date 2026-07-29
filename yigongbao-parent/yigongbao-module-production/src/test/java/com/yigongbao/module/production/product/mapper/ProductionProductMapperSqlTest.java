package com.yigongbao.module.production.product.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionProductMapperSqlTest {

    private static final String EXPORT_STATUS_PREDICATE =
            "pp.status IN ('in_process', 'fail', 'pass', 'pending_warehouse_in', 'warehoused', 'warehouse_out', 'completed')";

    @Test
    void exportQueriesIncludeEveryPostPrintProductStatus() {
        var exportQueries = Arrays.stream(ProductionProductMapper.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("listProductLedgerData")
                        || method.getName().equals("countProductLedgerData"))
                .map(method -> String.join(" ", method.getAnnotation(Select.class).value()))
                .toList();

        assertTrue(exportQueries.size() == 2, "expected both export queries to be present");
        for (String query : exportQueries) {
            assertTrue(query.contains(EXPORT_STATUS_PREDICATE),
                    () -> "export query does not use the complete post-print status predicate: " + query);
            assertTrue(!query.contains("pp.status IN ('in_process', 'fail', 'pass', 'completed')"),
                    () -> "export query still uses the old status predicate: " + query);
        }
    }
}
