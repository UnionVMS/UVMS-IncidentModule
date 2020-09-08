package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.*;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.ServiceConstants;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentRestResourceTest extends BuildIncidentTestDeployment {

    private static Jsonb jsonb;

    @Inject
    private JMSHelper jmsHelper;

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
    public void getStatuseThatCountAsResolved() {
        List<StatusEnum> response = getWebTarget()
                .path("incident/resolvedStatuses")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<List<StatusEnum>>() {});
        assertNotNull(response);
        assertEquals(ServiceConstants.RESOLVED_STATUS_LIST, response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentTypes() {
        List<IncidentType> response = getWebTarget()
                .path("incident/incidentTypes")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<List<IncidentType>>() {});
        assertNotNull(response);
        assertEquals(Arrays.asList(IncidentType.values()), response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        Instant expiryDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        incidentDto.setExpiryDate(expiryDate);
        IncidentDto createdIncident = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incidentDto), IncidentDto.class);

        assertNotNull(createdIncident.getId());
        assertEquals(incidentDto.getAssetId(), createdIncident.getAssetId());
        assertEquals(incidentDto.getType(), createdIncident.getType());
        assertEquals(expiryDate, createdIncident.getExpiryDate());
        assertNotNull(createdIncident.getUpdateDate());
        assertNotNull(createdIncident.getCreateDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentLogCreatedTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        IncidentDto createdIncident = createIncident(incidentDto);
        assertNotNull(createdIncident);

        Map<Long, IncidentLogDto> logs = getIncidentLogForIncident(createdIncident);

        assertEquals(1, logs.size());
        assertTrue(logs.values().stream()
                .anyMatch(log -> log.getMessage().contains(BuildIncidentTestDeployment.USER_NAME)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void addNoteCreatedEventToIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        IncidentDto createdIncident = createIncident(incidentDto);

        EventCreationDto creationDto = new EventCreationDto(EventTypeEnum.NOTE_CREATED, UUID.randomUUID());

        Response response = getWebTarget()
                .path("incident/addEventToIncident")
                .path(createdIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(creationDto), Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> logs = getIncidentLogForIncident(createdIncident);

        assertEquals(2, logs.size());
        assertTrue(logs.values().stream()
                .anyMatch(log -> log.getEventType().equals(creationDto.getEventType())));
        assertTrue(logs.values().stream()
                .anyMatch(log -> creationDto.getRelatedObjectId().equals(log.getRelatedObjectId())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void addPollCreatedEventToIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        IncidentDto createdIncident = createIncident(incidentDto);

        EventCreationDto creationDto = new EventCreationDto(EventTypeEnum.POLL_CREATED, UUID.randomUUID());

        Response response = getWebTarget()
                .path("incident/addEventToIncident")
                .path(createdIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(creationDto), Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> logs = getIncidentLogForIncident(createdIncident);

        assertEquals(2, logs.size());
        assertTrue(logs.values().stream()
                .anyMatch(log -> log.getEventType().equals(creationDto.getEventType())));
        assertTrue(logs.values().stream()
                .anyMatch(log -> creationDto.getRelatedObjectId().equals(log.getRelatedObjectId())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        IncidentDto createdIncident = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incidentDto), IncidentDto.class);

        createdIncident.setType(IncidentType.PARKED);
        Instant expiryDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        createdIncident.setExpiryDate(expiryDate);
        IncidentDto updatedIncident = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(createdIncident), IncidentDto.class);

        assertNotNull(updatedIncident.getId());
        assertEquals(incidentDto.getAssetId(), updatedIncident.getAssetId());
        assertEquals(IncidentType.PARKED, updatedIncident.getType());
        assertEquals(expiryDate, updatedIncident.getExpiryDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentLogCreatedTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNER_TRANSFER);
        IncidentDto createdIncident = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incidentDto), IncidentDto.class);

        createdIncident.setType(IncidentType.PARKED);
        Instant expiryDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        createdIncident.setExpiryDate(expiryDate);
        IncidentDto updatedIncident = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(createdIncident), IncidentDto.class);

        Map<Long, IncidentLogDto> logs = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path(updatedIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<Long, IncidentLogDto>>() {});

        assertEquals(2, logs.size());
        assertTrue(logs.values().stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_TYPE)));
    }


    @Test
    @OperateOnDeployment("incident")
    public void openIncidentsTest() {
        OpenAndRecentlyResolvedIncidentsDto response = getWebTarget()
                .path("incident/allOpenIncidents")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(OpenAndRecentlyResolvedIncidentsDto.class);
        assertNotNull(response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void noAuthTest() {
        Response response = getWebTarget()
                .path("incident/allOpenIncidents")
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        assertNotNull(response);
        assertEquals(403, response.getStatus());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentLogForIncidentTest() {
        Response response = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path("1")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
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
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentLogDto>>() {});
        assertNotNull(responseLogs);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentsForAssetTest() {
        Response response = getWebTarget()
                .path("incident/incidentsForAssetId")
                .path(UUID.randomUUID().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentDto>>() {});
        assertNotNull(responseLogs);
    }

    private IncidentDto createIncident(IncidentDto incident){
        return getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incident), IncidentDto.class);
    }

    private Map<Long, IncidentLogDto> getIncidentLogForIncident(IncidentDto incident){
        return getWebTarget()
                .path("incident/incidentLogForIncident")
                .path(incident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<Long, IncidentLogDto>>() {});
    }
}
