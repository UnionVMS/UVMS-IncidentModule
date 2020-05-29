package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class IncidentServiceBeanTest extends TransactionalTests {

    @Inject
    private JMSHelper jmsHelper;

    @Inject
    private IncidentServiceBean incidentService;

    private Jsonb jsonb;

    {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAssetNotSendingListTest() throws Exception {
        List<Incident> before = incidentService.getAssetNotSendingList();

        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(2000000000L);

        List<Incident> after = incidentService.getAssetNotSendingList();
        assertEquals(before.size() + 1, after.size());
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
        assertEquals(StatusEnum.POLL_FAILED, created.getStatus());

        created.setStatus(StatusEnum.RESOLVED);
        StatusDto status = new StatusDto();
        status.setStatus(StatusEnum.RESOLVED);
        incidentService.updateIncidentStatus(created.getId(), status);

        Incident updated = incidentService.findByTicketId(ticketId);
        assertEquals(updated.getStatus(), StatusEnum.RESOLVED);
    }

}
