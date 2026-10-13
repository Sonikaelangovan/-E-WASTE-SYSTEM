package com.loopcycle.item;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="ewaste_item")
public class EwasteItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="trace_id",nullable=false,unique=true) private String traceId;
 @Column(nullable=false,length=120) private String name;
 @Column(nullable=false,length=80) private String category;
 @Column(name="weight_kg",nullable=false,precision=12,scale=2) private BigDecimal weightKg;
 @Column(nullable=false,length=300) private String address;
 @Column(name="owner_email",length=254) private String ownerEmail;
 @Column(name="assigned_collector_email",length=254) private String assignedCollectorEmail;
 @Column(name="assigned_recycler_email",length=254) private String assignedRecyclerEmail;
 @Column(name="pickup_scheduled_at") private Instant pickupScheduledAt;
 @Column(length=1000) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private ItemStatus status;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @OneToMany(mappedBy="item",cascade=CascadeType.ALL,orphanRemoval=false) @OrderBy("createdAt ASC") private List<TraceEvent> history=new ArrayList<>();
 protected EwasteItem() {}
 public EwasteItem(String name,String category,BigDecimal weightKg,String address,String description,String ownerEmail){this.name=name;this.category=category;this.weightKg=weightKg;this.address=address;this.description=description;this.ownerEmail=ownerEmail;this.status=ItemStatus.SUBMITTED;this.createdAt=Instant.now();this.traceId="LC-"+java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase();}
 public void record(ItemStatus next,String actor,String notes){history.add(new TraceEvent(this,status,next,actor,notes,Instant.now()));status=next;}
 public void schedulePickup(String collectorEmail,Instant scheduledAt){this.assignedCollectorEmail=collectorEmail;this.pickupScheduledAt=scheduledAt;}
 public void assignRecycler(String recyclerEmail){this.assignedRecyclerEmail=recyclerEmail;}
 public Long getId(){return id;} public String getTraceId(){return traceId;} public String getName(){return name;} public String getCategory(){return category;} public BigDecimal getWeightKg(){return weightKg;} public String getAddress(){return address;} public String getDescription(){return description;} public String getOwnerEmail(){return ownerEmail;} public ItemStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public String getAssignedCollectorEmail(){return assignedCollectorEmail;} public String getAssignedRecyclerEmail(){return assignedRecyclerEmail;} public Instant getPickupScheduledAt(){return pickupScheduledAt;} public List<TraceEvent> getHistory(){return history;}
}
