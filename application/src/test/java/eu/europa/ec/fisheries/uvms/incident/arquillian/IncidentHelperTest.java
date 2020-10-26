package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.KeyValuePair;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.ServiceConstants;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentHelperTest extends TransactionalTests {

    @Inject
    IncidentHelper incidentHelper;

    @Test
    @OperateOnDeployment("incident")
    public void settingManualModeIntoParkedStatus() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.PARKED);
        try {
            incidentHelper.checkIfUpdateIsAllowed(incident, null);
            fail();
        }catch (Exception e){
            assertTrue(e.getMessage().contains("does not support being placed in status"));
        }
    }

    @Test
    @OperateOnDeployment("incident")
    public void givingAssetNotSendingAnExpiryDate() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.ASSET_NOT_SENDING);
        incident.setStatus(StatusEnum.INCIDENT_CREATED);
        incident.setExpiryDate(Instant.now());

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertNull(incident.getExpiryDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void setManualModeAsResolved() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.RESOLVED);
        incident.setExpiryDate(Instant.now());

       incidentHelper.setCorrectValuesForIncidentType(incident);
       assertEquals(StatusEnum.RESOLVED, incident.getStatus());
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkManualIncidentWoOldIncident() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.MANUAL_POSITION_MODE);
        Instant now = Instant.now();
        incident.setExpiryDate(now);

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertNotEquals(now, incident.getExpiryDate());
        assertTrue(now.plus(65, ChronoUnit.MINUTES).isBefore(incident.getExpiryDate()));
        assertTrue(now.plus(66, ChronoUnit.MINUTES).isAfter(incident.getExpiryDate()));
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkManualIncidentWithRecentMovement() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
        Instant now = Instant.now();
        incident.setExpiryDate(now);
        incident.setMovementId(UUID.randomUUID());

        Instant movementTimestamp = Instant.now().minus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES - 30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        System.setProperty("MOVEMENT_MOCK_TIMESTAMP", "" + movementTimestamp.toEpochMilli());

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertEquals(StatusEnum.MANUAL_POSITION_MODE, incident.getStatus());
        assertNotEquals(now, incident.getExpiryDate());
        assertEquals(movementTimestamp.plus(65, ChronoUnit.MINUTES), incident.getExpiryDate());

        System.clearProperty("MOVEMENT_MOCK_TIMESTAMP");
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkManualIncidentWithoutRecentMovement() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
        Instant now = Instant.now();
        incident.setExpiryDate(now);
        incident.setMovementId(UUID.randomUUID());

        Instant movementTimestamp = Instant.now().minus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES + 30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        System.setProperty("MOVEMENT_MOCK_TIMESTAMP", "" + movementTimestamp.toEpochMilli());

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertEquals(StatusEnum.MANUAL_POSITION_LATE, incident.getStatus());
        assertNotEquals(now, incident.getExpiryDate());
        assertEquals(movementTimestamp.plus(65, ChronoUnit.MINUTES), incident.getExpiryDate());

        System.clearProperty("MOVEMENT_MOCK_TIMESTAMP");
    }

    @Test
    @OperateOnDeployment("incident")
    public void createJsonStringTest() {
        String test = incidentHelper.createJsonString("test", Instant.now().toEpochMilli());
        assertNotNull(test);
        assertFalse(test.isEmpty());

        test = incidentHelper.createJsonString("test", "Instant.now().toEpochMilli()");
        assertNotNull(test);
        assertFalse(test.isEmpty());

        test = incidentHelper.createJsonString("test", Instant.now());
        assertNotNull(test);
        assertFalse(test.isEmpty());

        test = incidentHelper.createJsonString("test", IncidentType.MANUAL_POSITION_MODE);
        assertNotNull(test);
        assertFalse(test.isEmpty());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createJsonStringListTest() {
        List<KeyValuePair> keyValuePairs = Arrays.asList(new KeyValuePair("test1", Instant.now().toEpochMilli()),
                new KeyValuePair("test2", "Instant.now().toEpochMilli()"),
                new KeyValuePair("test3", Instant.now()),
                new KeyValuePair("test4", IncidentType.MANUAL_POSITION_MODE));
        String json = incidentHelper.createJsonString(keyValuePairs);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        assertTrue(json.contains("test1\":"));
        assertFalse(json.contains("test1\":\""));
        assertTrue(json.contains("test2\":\"Instant.now().toEpochMilli()\""));
        assertTrue(json.contains("test3\":"));
        assertFalse(json.contains("test3\":\""));
        assertTrue(json.contains("test4\":\"MANUAL_POSITION_MODE\""));

    }
}
