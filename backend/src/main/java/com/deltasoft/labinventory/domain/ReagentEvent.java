package com.deltasoft.labinventory.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "reagent_events",
        indexes = {
                @Index(name = "idx_reagent_events_reagent_id_created_at",
                        columnList = "reagent_id, created_at DESC")
        }
)
public class ReagentEvent {

    public enum EventType { CREATED, UPDATED, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reagent_id", nullable = false)
    private Long reagentId;

    @Column(name = "reagent_name", nullable = false)
    private String reagentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @Lob
    @Column(name = "changes_json")
    private String changesJson;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ReagentEvent() {}

    public ReagentEvent(Long reagentId, String reagentName, EventType eventType,
                        String changesJson, String actor, Instant createdAt) {
        this.reagentId = reagentId;
        this.reagentName = reagentName;
        this.eventType = eventType;
        this.changesJson = changesJson;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReagentId() { return reagentId; }
    public void setReagentId(Long reagentId) { this.reagentId = reagentId; }
    public String getReagentName() { return reagentName; }
    public void setReagentName(String reagentName) { this.reagentName = reagentName; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public String getChangesJson() { return changesJson; }
    public void setChangesJson(String changesJson) { this.changesJson = changesJson; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
