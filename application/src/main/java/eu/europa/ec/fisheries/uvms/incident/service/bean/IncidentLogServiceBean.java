package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.Instant;
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
        incidentLogDao.save(log);

    }

    public void createIncidentLogForManualPosition(Incident persisted, MicroMovement manual) {

        IncidentLog log = new IncidentLog();
        log.setCreateDate(Instant.now());
        log.setIncidentId(persisted.getId());
        log.setEventType(EventTypeEnum.MANUEL_POSITION);
        log.setRelatedObjectId(UUID.fromString(manual.getId()));
        log.setMessage(EventTypeEnum.MANUEL_POSITION.getMessage());
        incidentLogDao.save(log);
    }
}
