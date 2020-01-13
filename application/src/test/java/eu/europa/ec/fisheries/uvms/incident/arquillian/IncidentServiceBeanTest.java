package eu.europa.ec.fisheries.uvms.incident.arquillian;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.enums.StatusEnum;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentServiceBeanTest extends TransactionalTests {

    @Inject
    private TicketHelper ticketHelper;

    @Inject
    private JMSHelper jmsHelper;

    @Inject
    private IncidentServiceBean incidentService;

    private ObjectMapper objectMapper = getObjectMapper();

    @Test
    @OperateOnDeployment("incident")
    public void getAssetNotSendingListTest() throws Exception {
        List<Incident> before = incidentService.getAssetNotSendingList();

        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        TicketType ticket = ticketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = objectMapper.writeValueAsString(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(5000000000L);

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
        TicketType ticket = ticketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = objectMapper.writeValueAsString(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(5000000000L);

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
        TicketType ticket = ticketHelper.createTicket(ticketId, assetId, movementId, mobTermId);
        String asString = objectMapper.writeValueAsString(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString);

        LockSupport.parkNanos(5000000000L);

        Incident created = incidentService.findByTicketId(ticketId);
        assertEquals(StatusEnum.POLL_FAILED, created.getStatus());

        created.setStatus(StatusEnum.RESOLVED);
        StatusDto status = new StatusDto();
        status.setStatus("RESOLVED");
        incidentService.updateIncidentStatus(created.getId(), status);

        Incident updated = incidentService.findByTicketId(ticketId);
        assertEquals(updated.getStatus(), StatusEnum.RESOLVED);
    }

}
