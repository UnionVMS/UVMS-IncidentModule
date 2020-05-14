package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TopicListener;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentLogDto;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class IncidentResourceTest extends BuildIncidentTestDeployment {

    UUID ticketId;
    UUID assetId;
    UUID movId;
    UUID mobTermId;
    TicketType ticket;
    IncidentDto incident;

    private Jsonb jsonb;

    @Inject
    private JMSHelper jmsHelper;

    @BeforeClass
    public void consumeIncidentQueue() throws Exception {
        jmsHelper.clearQueue(jmsHelper.QUEUE_NAME);
        jsonb = new JsonBConfigurator().getContext(null);

        ticketId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        movId = UUID.randomUUID();
        mobTermId = UUID.randomUUID();
        ticket = TicketHelper.createTicket(ticketId, assetId, movId, mobTermId);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = jsonb.toJson(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString);

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            incident = jsonb.fromJson(text, IncidentDto.class);
            assertEquals(assetId, incident.getAssetId());
        }
    }

    @Test
    @OperateOnDeployment("incident")
    public void assetNotSendingTest() {
        List<IncidentDto> response = getWebTarget()
                .path("incident/assetNotSending")
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<>() {});
        assertNotNull(response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentLogForIncidentTest() {
        Response response = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path("" + incident.getId())
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        assertEquals(200, response.getStatus());

        List<IncidentLogDto> responseLogs = response.readEntity(new GenericType<List<IncidentLogDto>>() {});
    }
}
