package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class IncidentLogDaoTest extends TransactionalTests {

    @Inject
    IncidentLogDao incidentLogDao;

    @Test
    @OperateOnDeployment("incident")
    public void checkIfMovementAlreadyExistsTest() {
        UUID movementId = UUID.randomUUID();
        Long incidentId = (long)(Math.random() * 10000d);

        IncidentLog incidentLog = new IncidentLog();
        incidentLog.setRelatedObjectId(movementId);
        incidentLog.setIncidentId(incidentId);
        incidentLog.setCreateDate(Instant.now());
        incidentLog.setMessage("test message");
        incidentLog.setEventType(EventTypeEnum.MANUAL_POSITION);

        incidentLogDao.save(incidentLog);

        assertTrue(incidentLogDao.checkIfMovementAlreadyExistsForIncident(incidentId, movementId));
    }
}
