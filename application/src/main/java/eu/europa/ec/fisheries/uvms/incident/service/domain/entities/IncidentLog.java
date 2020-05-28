package eu.europa.ec.fisheries.uvms.incident.service.domain.entities;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_log")
@NamedQueries({
        @NamedQuery(name = IncidentLog.FIND_ALL_BY_INCIDENT_ID, query = "SELECT i FROM IncidentLog i WHERE i.incidentId = :incidentId"),
        @NamedQuery(name = IncidentLog.FIND_ALL_BY_INCIDENT_ID_LIST, query = "SELECT i FROM IncidentLog i WHERE i.incidentId in :incidentId"),
})
public class IncidentLog {

    public static final String FIND_ALL_BY_INCIDENT_ID = "IncidentLog.findByIncidentId";
    public static final String FIND_ALL_BY_INCIDENT_ID_LIST = "IncidentLog.findByIncidentIdList";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", name = "id")
    private long id;

    @NotNull
    @Column(name = "incident_id")
    private long incidentId;

    @NotNull
    @Column(name = "message")
    private String message;

    @NotNull
    @Column(name = "event_type")
    @Enumerated(value = EnumType.STRING)
    private EventTypeEnum eventType;

    @Column(name = "related_object_id")
    private UUID relatedObjectId;

    @NotNull
    @Column(name = "create_date")
    private Instant createDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

}
