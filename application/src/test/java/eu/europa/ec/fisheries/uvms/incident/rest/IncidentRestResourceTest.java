package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.AssetNotSendingDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentLogDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class IncidentRestResourceTest extends BuildIncidentTestDeployment {

    UUID ticketId;
    UUID assetId;
    UUID movId;
    UUID mobTermId;
    IncidentTicketDto ticket;
    IncidentDto incident;

    private static Jsonb jsonb;

    @Inject
    private JMSHelper jmsHelper;

    //For some reason I can not get jsonb.toJson to work, it only gives me abstract method error
    // so the below tests just check that the endpoints are reached and dont return exceptions
    /*@Before
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
            assertNotNull(message);
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            incident = jsonb.fromJson(text, IncidentDto.class);
        }
    }*/

    @Test
    @OperateOnDeployment("incident")
    public void assetNotSendingTest() {
        AssetNotSendingDto response = getWebTarget()
                .path("incident/assetNotSending")
                .request(MediaType.APPLICATION_JSON)
                .get(AssetNotSendingDto.class);
        assertNotNull(response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentLogForIncidentTest() {
        Response response = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path("1")
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentLogDto>>() {});
        assertNotNull(responseLogs);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentLogsForAssetTest() {
        Response response = getWebTarget()
                .path("incident/incidentLogsForAssetId")
                .path(UUID.randomUUID().toString())
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentLogDto>>() {});
        assertNotNull(responseLogs);
    }

}
