package com.loopcycle.item;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="trace_event")
public class TraceEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="item_id",nullable=false) private EwasteItem item;
 @Enumerated(EnumType.STRING) @Column(name="previous_status") private ItemStatus previousStatus;
 @Enumerated(EnumType.STRING) @Column(name="to_status",nullable=false) private ItemStatus toStatus;
 @Column(nullable=false,length=254) private String actor;
 @Column(length=1000) private String notes;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected TraceEvent() {}
 public TraceEvent(EwasteItem item, ItemStatus previousStatus, ItemStatus toStatus, String actor, String notes, Instant createdAt) {this.item=item;this.previousStatus=previousStatus;this.toStatus=toStatus;this.actor=actor;this.notes=notes;this.createdAt=createdAt;}
 public Long getId(){return id;} public ItemStatus getPreviousStatus(){return previousStatus;} public ItemStatus getToStatus(){return toStatus;} public String getActor(){return actor;} public String getNotes(){return notes;} public Instant getCreatedAt(){return createdAt;}
}
