package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.*;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.rest.filters.AppError;
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
    public void getValidStatusForTypes() {
        Map<IncidentType, List<StatusEnum>> response = getWebTarget()
                .path("incident/validStatusForTypes")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<IncidentType, List<StatusEnum>>>() {});
        assertNotNull(response);
        for (IncidentType value : IncidentType.values()) {
            assertTrue(value.name(), value.getValidStatuses().equals(response.get(value.name())));
        }
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

        IncidentDto createdIncident = createIncident(incidentDto);

        assertNotNull(createdIncident.getId());
        assertEquals(incidentDto.getAssetId(), createdIncident.getAssetId());
        assertEquals(incidentDto.getType(), createdIncident.getType());
        assertEquals(expiryDate, createdIncident.getExpiryDate());
        assertNotNull(createdIncident.getUpdateDate());
        assertNotNull(createdIncident.getCreateDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentWithInvalidStatus() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.PARKED);

        AppError error = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incidentDto), AppError.class);

        assertTrue(error.description, error.description.contains("does not support being placed in status"));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentThatShouldNotHaveAnEndDateWithOne() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.INCIDENT_CREATED);
        incidentDto.setExpiryDate(Instant.now());

        AppError error = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incidentDto), AppError.class);

        assertTrue(error.description, error.description.contains("does not support having a expiry date"));
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
        incidentDto.setStatus(null);
        IncidentDto createdIncident = createIncident(incidentDto);

        assertEquals(StatusEnum.INCIDENT_CREATED, createdIncident.getStatus());

        createdIncident.setType(IncidentType.PARKED);
        createdIncident.setStatus(null);
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
        assertEquals(StatusEnum.PARKED, updatedIncident.getStatus());
        assertEquals(expiryDate, updatedIncident.getExpiryDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentInvalidStatus() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.ATTEMPTED_CONTACT);
        IncidentDto createdIncident = createIncident(incidentDto);

        createdIncident.setStatus(StatusEnum.PARKED);
        AppError error = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(createdIncident), AppError.class);

        assertNotNull(error);
        assertTrue(error.description, error.description.contains("does not support being placed in status"));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentInvalidTypeForExpiryDate() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.ATTEMPTED_CONTACT);
        IncidentDto createdIncident = createIncident(incidentDto);

        createdIncident.setExpiryDate(Instant.now());
        AppError error = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(createdIncident), AppError.class);

        assertNotNull(error);
        assertTrue(error.description, error.description.contains("does not support having a expiry date"));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentLogCreatedTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);
        IncidentDto createdIncident = createIncident(incidentDto);

        createdIncident.setType(IncidentType.PARKED);
        createdIncident.setStatus(StatusEnum.PARKED);
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

    @Test
    @OperateOnDeployment("incident")
    public void getAllIncidentsForAsset() {
        IncidentDto incidentDto1 = TicketHelper.createBasicIncidentDto();
        UUID assetId = incidentDto1.getAssetId();
        IncidentDto createdIncident1 = createIncident(incidentDto1);

        createdIncident1.setType(IncidentType.PARKED);
        createdIncident1.setStatus(StatusEnum.RESOLVED);
        IncidentDto closedIncident = updateIncident(createdIncident1);

        IncidentDto incidentDto2 = TicketHelper.createBasicIncidentDto();
        incidentDto2.setAssetId(assetId);
        IncidentDto createdIncident2 = createIncident(incidentDto2);

        Response response = getWebTarget()
                .path("incident/incidentsForAssetId")
                .path(assetId.toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);

        Map<Long, IncidentDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentDto>>() {});
        assertNotNull(responseLogs);
        assertEquals(2, responseLogs.size());
        assertTrue(responseLogs.containsKey("" + closedIncident.getId()));
        assertTrue(responseLogs.containsKey("" + createdIncident2.getId()));
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsForAsset() {
        IncidentDto incidentDto1 = TicketHelper.createBasicIncidentDto();
        UUID assetId = incidentDto1.getAssetId();
        IncidentDto createdIncident1 = createIncident(incidentDto1);

        createdIncident1.setType(IncidentType.PARKED);
        createdIncident1.setStatus(StatusEnum.RESOLVED);
        IncidentDto closedIncident = updateIncident(createdIncident1);

        IncidentDto incidentDto2 = TicketHelper.createBasicIncidentDto();
        incidentDto2.setAssetId(assetId);
        IncidentDto createdIncident2 = createIncident(incidentDto2);

        Response response = getWebTarget()
                .path("incident/incidentsForAssetId")
                .path(assetId.toString())
                .queryParam("onlyOpen", "true")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);

        Map<Long, IncidentDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentDto>>() {});
        assertNotNull(responseLogs);
        assertEquals(1, responseLogs.size());
        assertFalse(responseLogs.containsKey("" + closedIncident.getId()));
        assertTrue(responseLogs.containsKey("" + createdIncident2.getId()));
    }

    private IncidentDto createIncident(IncidentDto incident){
        return getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incident), IncidentDto.class);
    }

    private IncidentDto updateIncident(IncidentDto incident){
        return getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(incident), IncidentDto.class);
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
