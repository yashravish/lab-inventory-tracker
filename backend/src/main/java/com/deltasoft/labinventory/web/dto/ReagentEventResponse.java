package com.deltasoft.labinventory.web.dto;

import com.deltasoft.labinventory.domain.ReagentEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ReagentEventResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, List<String>>> CHANGES_TYPE =
            new TypeReference<>() {};

    private Long id;
    private Long reagentId;
    private String reagentName;
    private ReagentEvent.EventType eventType;
    private Map<String, List<String>> changes;
    private String actor;
    private Instant createdAt;

    public static ReagentEventResponse from(ReagentEvent e) {
        ReagentEventResponse dto = new ReagentEventResponse();
        dto.id = e.getId();
        dto.reagentId = e.getReagentId();
        dto.reagentName = e.getReagentName();
        dto.eventType = e.getEventType();
        dto.actor = e.getActor();
        dto.createdAt = e.getCreatedAt();
        if (e.getEventType() == ReagentEvent.EventType.UPDATED
                && e.getChangesJson() != null && !e.getChangesJson().isBlank()) {
            try {
                dto.changes = MAPPER.readValue(e.getChangesJson(), CHANGES_TYPE);
            } catch (Exception ignored) {
                dto.changes = null;
            }
        }
        return dto;
    }

    public Long getId() { return id; }
    public Long getReagentId() { return reagentId; }
    public String getReagentName() { return reagentName; }
    public ReagentEvent.EventType getEventType() { return eventType; }
    public Map<String, List<String>> getChanges() { return changes; }
    public String getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
