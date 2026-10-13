package com.loopcycle.report;

import com.loopcycle.item.ItemStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReportSummary(long totalItems, BigDecimal totalWeightKg, long recycledItems,
                            long disposedItems, Map<ItemStatus, Long> byStatus,
                            List<CategoryTotal> byCategory) {
    public record CategoryTotal(String category, long itemCount, BigDecimal weightKg) {}
}
