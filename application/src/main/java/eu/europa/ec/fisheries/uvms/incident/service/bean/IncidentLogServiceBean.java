package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.model.dto.MicroMovementDto;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.domain.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.time.Instant;
import java.util.List;

@Stateless
public class IncidentLogServiceBean {
    private static final Logger LOG = LoggerFactory.getLogger(IncidentLogServiceBean.class);

    @Inject
    private IncidentLogDao incidentLogDao;
    @Inject
    private IncidentHelper incidentHelper;

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    public List<IncidentLog> getAssetNotSendingEventChanges(long incidentId) {
        return incidentLogDao.findAllByIncidentId(incidentId);
    }

    public void createIncidentLogForStatus(String oldValue, Incident updated) {
        IncidentLog log = new IncidentLog();
        log.setCreateDate(Instant.now());
        log.setIncidentId(updated.getId());
        log.setEventType(EventTypeEnum.INCIDENT_STATUS);
        log.setPreviousValue(oldValue);
        log.setCurrentValue(updated.getStatus().name());
        log.setMessage(EventTypeEnum.INCIDENT_STATUS.getMessage());
        incidentLogDao.save(log);

    }

    public void createIncidentLogForManualPosition(Incident persisted, MicroMovement manual, MicroMovement latest) {
        MicroMovementDto manualPositionDto = incidentHelper.mapToMicroMovementDto(manual);
        MicroMovementDto latestPositionDto = incidentHelper.mapToMicroMovementDto(latest);

        try {
            String jsonPrevious = jsonb.toJson(latestPositionDto);
            String jsonCurrent = jsonb.toJson(manualPositionDto);
            IncidentLog log = new IncidentLog();
            log.setCreateDate(Instant.now());
            log.setIncidentId(persisted.getId());
            log.setEventType(EventTypeEnum.MANUEL_POSITION);
            log.setPreviousValue(jsonPrevious);
            log.setCurrentValue(jsonCurrent);
            log.setMessage(EventTypeEnum.MANUEL_POSITION.getMessage());
            incidentLogDao.save(log);
        } catch (Exception e) {
            LOG.error("Error when creating MicroMovementExtended JSON object: " + e.getMessage(), e);
        }
    }
}
