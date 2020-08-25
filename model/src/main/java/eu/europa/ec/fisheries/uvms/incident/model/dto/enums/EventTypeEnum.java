package eu.europa.ec.fisheries.uvms.incident.model.dto.enums;

public enum EventTypeEnum {
    MANUAL_POSITION("New Manual position created", RelatedObjectType.MOVEMENT),
    MANUAL_POSITION_LATE("No new manual position in 65 minutes while in manual position mode", RelatedObjectType.NONE),
    POLL_CREATED("Poll has been created", RelatedObjectType.POLL),
    AUTO_POLL_CREATED("Automatic poll has been created", RelatedObjectType.POLL),
    NOTE_CREATED("Note has been created", RelatedObjectType.NOTE),
    INCIDENT_CLOSED("Incident has been closed", RelatedObjectType.MOVEMENT),
    INCIDENT_STATUS("Incident Status updated", RelatedObjectType.NONE);

    private String message;
    private RelatedObjectType relatedObjectType;

    EventTypeEnum(String message, RelatedObjectType relatedObjectType) {
        this.message = message;
        this.relatedObjectType = relatedObjectType;
    }

    public String getMessage() {
        return message;
    }

    public RelatedObjectType getRelatedObjectType() {
        return relatedObjectType;
    }
}

