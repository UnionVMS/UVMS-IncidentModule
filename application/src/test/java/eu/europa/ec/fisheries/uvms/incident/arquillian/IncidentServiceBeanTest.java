package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.model.dto.OpenAndRecentlyResolvedIncidentsDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.EventCreationDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.MovementSourceType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    @Inject
    IncidentHelper incidentHelper;

    private Jsonb jsonb;

    {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsTest() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        OpenAndRecentlyResolvedIncidentsDto after = incidentService.getAllOpenAndRecentlyResolvedIncidents();
        assertEquals(before.getUnresolved().size() + 1, after.getUnresolved().size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsSeveralIncidentTypesTest() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        IncidentTicketDto ticket = TicketHelper.createTicket(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        ticket = TicketHelper.createTicket(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ticket.setType(IncidentType.PARKED);
        asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        OpenAndRecentlyResolvedIncidentsDto after = incidentService.getAllOpenAndRecentlyResolvedIncidents();
        assertEquals(before.getUnresolved().size() + 2, after.getUnresolved().size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void ignoreMessageWOOpenIncidentAndWoType() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        List<Incident> incidents = incidentDao.findByAssetId(assetId);
        assertTrue(incidents.isEmpty());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentByTicketIdTest() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        ticket.setId(ticketId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

        LockSupport.parkNanos(2000000000L);

        Incident incident = incidentService.findByTicketId(ticketId);
        assertNotNull(incident);
        assertEquals(assetId, incident.getAssetId());
        assertEquals(movementId, incident.getMovementId());
    }

    @Test
    @OperateOnDeployment("incident")
    public void setIncidentTypeToManual() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        incidentService.createIncident(ticket);

        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertEquals(assetId, incident.getAssetId());
        assertEquals(IncidentType.MANUAL_MODE, incident.getType());
    }

    @Test
    @OperateOnDeployment("incident")
    public void setIncidentTypeToManualCheckLog() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        incidentService.createIncident(ticket);

        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertTrue(incident.getExpiryDate().isAfter(Instant.now().plus(64, ChronoUnit.MINUTES)));
        assertTrue(incident.getExpiryDate().isBefore(Instant.now().plus(66, ChronoUnit.MINUTES)));

        assertTrue(incidentLogDao.checkIfMovementAlreadyExistsForIncident(incident.getId(), UUID.fromString(ticket.getMovementId())));
        List<IncidentLog> incidentLog = incidentLogDao.findAllByIncidentId(incident.getId());

        IncidentLog manualMovementLog = incidentLog.stream().filter(log -> UUID.fromString(ticket.getMovementId()).equals(log.getRelatedObjectId())).findAny().get();
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, manualMovementLog.getIncidentStatus());
        assertEquals(EventTypeEnum.MANUAL_POSITION, manualMovementLog.getEventType());
    }


    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentTest() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

        LockSupport.parkNanos(2000000000L);

        Incident created = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(created);
        assertEquals(StatusEnum.INCIDENT_CREATED, created.getStatus());

        IncidentDto incidentDto = incidentHelper.incidentEntityToDto(created);
        incidentDto.setStatus(StatusEnum.RESOLVED);
        IncidentDto updatedDto = incidentService.updateIncident(incidentDto, "Test user");

        Incident updated = incidentDao.findById(created.getId());
        assertEquals(updated.getStatus(), StatusEnum.RESOLVED);

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(updated.getId());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CLOSED)));

    }

    @Test
    @OperateOnDeployment("incident")
    public void createAssetSendingDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending ais despite parked");
        ticket.setRuleGuid("Asset sending ais despite parked");
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
    public void createAndUpdateAssetSendingAisDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending ais despite parked");
        ticket.setRuleGuid("Asset sending ais despite parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);

        UUID updatedMovement = new UUID(0l, movementId.getMostSignificantBits());
        ticket.setMovementId(updatedMovement.toString());
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());
        ticket.setType(null);

        incidentService.updateIncident(ticket);

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getMessage().contains(ticket.getRuleGuid())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndCloseAssetSendingVmsDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending ais despite parked");
        ticket.setRuleGuid("Asset sending ais despite parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);

        UUID updatedMovement = new UUID(0l, movementId.getMostSignificantBits());
        ticket.setMovementId(updatedMovement.toString());
        ticket.setMovementSource(MovementSourceType.NAF);
        ticket.setUpdated(Instant.now());
        ticket.setType(null);

        incidentService.updateIncident(ticket);
        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RESOLVED, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CLOSED)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingAisDespiteOwnerTransferTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNER_TRANSFER);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNER_TRANSFER);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.PARKED, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingVmsDespiteOwnerTransferTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNER_TRANSFER);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNER_TRANSFER);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.IRIDIUM);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.PARKED, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingAisDespiteSeasonalFishingTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.SEASONAL_FISHING);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_AIS_POSITIONS, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndCloseAssetSendingVmsDespiteSeasonalFishingTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.SEASONAL_FISHING);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.IRIDIUM);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RESOLVED, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.RESOLVED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesManualPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.MANUAL);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_MODE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.MANUAL_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesAssetNotSendingShouldDoNothing() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        ticket.setMovementSource(MovementSourceType.MANUAL);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(1, incidentLogs.size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesAisPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_MODE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesSeveralAisPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_MODE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesSeveralAisPositionWith62MinutesBetweenThem() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        IncidentLog incidentLog = incidentLogs.stream().filter(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)).findAny().get();
        incidentLog.setCreateDate(Instant.now().minus(62, ChronoUnit.MINUTES));
        incidentLogDao.update(incidentLog);

        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesVmsPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.OTHER);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_VMS_POSITIONS, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.RECEIVING_VMS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }
}
