package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.RelatedObjectType;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentLogServiceBean {
    private static final Logger LOG = LoggerFactory.getLogger(IncidentLogServiceBean.class);

    @Inject
    private IncidentLogDao incidentLogDao;
    @Inject
    private IncidentHelper incidentHelper;


    public List<IncidentLog> getIncidentLogByIncidentId(long incidentId) {
        return incidentLogDao.findAllByIncidentId(incidentId);
    }

    public void createIncidentLogForStatus(Incident updated, String message, EventTypeEnum eventType, UUID relatedObjectId) {
        IncidentLog log = new IncidentLog();
        log.setCreateDate(Instant.now());
        log.setIncidentId(updated.getId());
        log.setEventType(eventType);
        log.setRelatedObjectId(relatedObjectId);
        log.setMessage(message);
        log.setIncidentStatus(updated.getStatus());
        log.setRelatedObjectType( relatedObjectId == null ? RelatedObjectType.NONE :  eventType.getRelatedObjectType());
        incidentLogDao.save(log);

    }

    public void createIncidentLogForManualPosition(Incident persisted, UUID manualId) {

        IncidentLog log = new IncidentLog();
        log.setCreateDate(Instant.now());
        log.setIncidentId(persisted.getId());
        log.setEventType(EventTypeEnum.MANUAL_POSITION);
        log.setRelatedObjectId(manualId);
        log.setMessage(EventTypeEnum.MANUAL_POSITION.getMessage());
        log.setIncidentStatus(persisted.getStatus());
        log.setRelatedObjectType(RelatedObjectType.MOVEMENT);
        incidentLogDao.save(log);
    }

    public IncidentLog findLogWithTypeEntryFromTheLastHour(long incidentId, EventTypeEnum eventType){
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        List<IncidentLog> incidentLogs = incidentLogDao.findLogWithEventTypeAfter(incidentId, eventType, hourAgo);
        return incidentLogs.isEmpty() ? null : incidentLogs.get(0);
    }

    public IncidentLog findLogWithTypeEntryFromTheLastDay(long incidentId, EventTypeEnum eventType){
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        List<IncidentLog> incidentLogs = incidentLogDao.findLogWithEventTypeAfter(incidentId, eventType, hourAgo);
        return incidentLogs.isEmpty() ? null : incidentLogs.get(0);
    }
}
