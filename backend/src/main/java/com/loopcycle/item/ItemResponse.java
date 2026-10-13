package com.loopcycle.item;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record ItemResponse(Long id,String traceId,String name,String category,BigDecimal weightKg,String address,String description,ItemStatus status,Instant createdAt,String assignedCollectorEmail,String assignedRecyclerEmail,Instant pickupScheduledAt,List<TraceResponse> history){
 static ItemResponse from(EwasteItem item){return new ItemResponse(item.getId(),item.getTraceId(),item.getName(),item.getCategory(),item.getWeightKg(),item.getAddress(),item.getDescription(),item.getStatus(),item.getCreatedAt(),item.getAssignedCollectorEmail(),item.getAssignedRecyclerEmail(),item.getPickupScheduledAt(),item.getHistory().stream().map(e->new TraceResponse(e.getId(),e.getPreviousStatus(),e.getToStatus(),e.getActor(),e.getNotes(),e.getCreatedAt())).toList());}
}
record TraceResponse(Long id,ItemStatus previousStatus,ItemStatus toStatus,String actor,String notes,Instant createdAt){}
