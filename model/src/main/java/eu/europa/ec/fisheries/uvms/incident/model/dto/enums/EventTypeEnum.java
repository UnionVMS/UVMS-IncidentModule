package eu.europa.ec.fisheries.uvms.incident.model.dto.enums;

public enum EventTypeEnum {
    MANUAL_POSITION("New Manual position created"),
    POLL_CREATED("Poll has been created"),
    AUTO_POLL_CREATED("Automatic poll has been created"),
    NOTE_CREATED("Note has been created"),
    INCIDENT_CLOSED("Incident has been closed"),
    INCIDENT_STATUS("Incident Status updated");

    private String message;

    EventTypeEnum(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

