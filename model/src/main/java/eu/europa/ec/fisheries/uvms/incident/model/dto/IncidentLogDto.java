package eu.europa.ec.fisheries.uvms.incident.model.dto;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.RelatedObjectType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class IncidentLogDto implements Serializable {
    private final static long serialVersionUID = 1L;

    private Long id;
    private long incidentId;
    private String message;
    private EventTypeEnum eventType;
    private Instant createDate;
    private UUID relatedObjectId;
    private RelatedObjectType relatedObjectType;
    private StatusEnum incidentStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public void setEventType(EventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public UUID getRelatedObjectId() {
        return relatedObjectId;
    }

    public void setRelatedObjectId(UUID relatedObjectId) {
        this.relatedObjectId = relatedObjectId;
    }

    public RelatedObjectType getRelatedObjectType() {
        return relatedObjectType;
    }

    public void setRelatedObjectType(RelatedObjectType relatedObjectType) {
        this.relatedObjectType = relatedObjectType;
    }

    public StatusEnum getIncidentStatus() {
        return incidentStatus;
    }

    public void setIncidentStatus(StatusEnum incidentStatus) {
        this.incidentStatus = incidentStatus;
    }
}
