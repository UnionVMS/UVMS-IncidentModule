package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.AssetNotSendingDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentServiceBeanTest extends TransactionalTests {

    @Inject
    private JMSHelper jmsHelper;

    @Inject
    private IncidentServiceBean incidentService;

    @Inject
    private IncidentDao incidentDao;

    @Inject
    private IncidentLogDao incidentLogDao;

    private Jsonb jsonb;

    {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAssetNotSendingListTest() throws Exception {
        AssetNotSendingDto before = incidentService.getAssetNotSendingList();

        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(2000000000L);

        AssetNotSendingDto after = incidentService.getAssetNotSendingList();
        assertEquals(before.getUnresolved().size() + 1, after.getUnresolved().size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentByTicketIdTest() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(2000000000L);

        Incident incident = incidentService.findByTicketId(ticketId);
        assertNotNull(incident);
        assertEquals(assetId, incident.getAssetId());
        assertEquals(movementId, incident.getMovementId());
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentTest() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(2000000000L);

        Incident created = incidentService.findByTicketId(ticketId);
        assertNotNull(created);
        assertEquals(StatusEnum.INCIDENT_CREATED, created.getStatus());

        created.setStatus(StatusEnum.RESOLVED);
        StatusDto status = new StatusDto();
        status.setStatus(StatusEnum.RESOLVED);
        status.setEventType(EventTypeEnum.INCIDENT_CLOSED);
        incidentService.updateIncidentStatus(created.getId(), status);

        Incident updated = incidentService.findByTicketId(ticketId);
        assertEquals(updated.getStatus(), StatusEnum.RESOLVED);
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAssetSendingDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(null, assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending despite long term parked");
        ticket.setRuleGuid("Asset sending despite long term parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);
        assertEquals(movementId, openByAssetAndType.getMovementId());
        assertEquals(mobTermId, openByAssetAndType.getMobileTerminalId());
        assertNull(openByAssetAndType.getTicketId());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingDespiteLongTermParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(null, assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending despite long term parked");
        ticket.setRuleGuid("Asset sending despite long term parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);

        UUID updatedMovement = new UUID(0l, movementId.getMostSignificantBits());
        ticket.setMovementId(updatedMovement.toString());
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().allMatch(log -> log.getMessage().contains(ticket.getRuleGuid())));
    }

}
