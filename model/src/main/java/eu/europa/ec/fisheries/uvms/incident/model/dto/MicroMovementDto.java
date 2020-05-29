package eu.europa.ec.fisheries.uvms.incident.model.dto;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.MovementSourceType;

import java.time.Instant;


public class MicroMovementDto {

    private MovementPointDto location;

    private Double heading;

    private String id;

    private Instant timestamp;

    private Double speed;

    private MovementSourceType source;

    public MovementPointDto getLocation() {
        return location;
    }

    public void setLocation(MovementPointDto location) {
        this.location = location;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public MovementSourceType getSource() {
        return source;
    }

    public void setSource(MovementSourceType source) {
        this.source = source;
    }
}
