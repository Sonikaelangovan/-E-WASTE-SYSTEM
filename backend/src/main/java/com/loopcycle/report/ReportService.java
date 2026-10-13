package com.loopcycle.report;

import com.loopcycle.item.EwasteItem;
import com.loopcycle.item.ItemRepository;
import com.loopcycle.item.ItemStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReportService {
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ISO_INSTANT;
    private final ItemRepository items;

    public ReportService(ItemRepository items) { this.items = items; }

    public ReportSummary summary(LocalDate from, LocalDate to, ItemStatus status, Authentication actor) {
        List<EwasteItem> filtered = filter(from, to, status, actor);
        BigDecimal weight = filtered.stream().map(EwasteItem::getWeightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<ItemStatus, Long> statusCounts = new EnumMap<>(ItemStatus.class);
        Map<String, List<EwasteItem>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (EwasteItem item : filtered) {
            statusCounts.merge(item.getStatus(), 1L, Long::sum);
            grouped.computeIfAbsent(item.getCategory(), ignored -> new ArrayList<>()).add(item);
        }
        List<ReportSummary.CategoryTotal> categories = grouped.entrySet().stream().map(entry -> {
            BigDecimal categoryWeight = entry.getValue().stream().map(EwasteItem::getWeightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new ReportSummary.CategoryTotal(entry.getKey(), entry.getValue().size(), scale(categoryWeight));
        }).toList();
        return new ReportSummary(filtered.size(), scale(weight),
                statusCounts.getOrDefault(ItemStatus.RECYCLED, 0L), statusCounts.getOrDefault(ItemStatus.DISPOSED, 0L),
                statusCounts, categories);
    }

    public byte[] csv(LocalDate from, LocalDate to, ItemStatus status, Authentication actor) {
        StringBuilder csv = new StringBuilder("Trace ID,Category,Weight kg,Status,Submitted at\r\n");
        for (EwasteItem item : filter(from, to, status, actor)) {
            csv.append(cell(item.getTraceId())).append(',')
                    .append(cell(item.getCategory())).append(',')
                    .append(cell(item.getWeightKg().toPlainString())).append(',')
                    .append(cell(item.getStatus().name())).append(',')
                    .append(cell(CSV_DATE.format(item.getCreatedAt()))).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<EwasteItem> filter(LocalDate from, LocalDate to, ItemStatus status, Authentication actor) {
        if (from != null && to != null && to.isBefore(from))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The end date must be on or after the start date");
        boolean citizen = actor.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_CITIZEN"));
        List<EwasteItem> visible = citizen ? items.findAllByOwnerEmailForReporting(actor.getName()) : items.findAllForReporting();
        Instant start = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endExclusive = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return visible.stream()
                .filter(item -> start == null || !item.getCreatedAt().isBefore(start))
                .filter(item -> endExclusive == null || item.getCreatedAt().isBefore(endExclusive))
                .filter(item -> status == null || item.getStatus() == status)
                .sorted(Comparator.comparing(EwasteItem::getCreatedAt).reversed())
                .toList();
    }

    private BigDecimal scale(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    private String cell(String value) {
        String safe = value == null ? "" : value;
        if (safe.stripLeading().matches("^[=+@\\-].*")) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
