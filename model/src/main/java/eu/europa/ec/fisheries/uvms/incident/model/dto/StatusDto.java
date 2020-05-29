package eu.europa.ec.fisheries.uvms.incident.model.dto;

import java.io.Serializable;
import java.util.UUID;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.*;

public class StatusDto implements Serializable {

    private StatusEnum status;
    private EventTypeEnum eventType;
    private UUID relatedObjectId;

    public StatusDto() {
    }

    public StatusDto(StatusEnum status, EventTypeEnum eventType, UUID relatedObjectId) {
        this.status = status;
        this.eventType = eventType;
        this.relatedObjectId = relatedObjectId;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public void setEventType(EventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public UUID getRelatedObjectId() {
        return relatedObjectId;
    }

    public void setRelatedObjectId(UUID relatedObjectId) {
        this.relatedObjectId = relatedObjectId;
    }
}
