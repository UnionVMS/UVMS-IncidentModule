package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.uvms.incident.model.dto.OpenAndRecentlyResolvedIncidentsDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.EventCreationDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.*;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentCreate;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentUpdate;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentServiceBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentServiceBean.class);

    private static final String uuidPattern = "[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}";

    @Inject
    private IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    private IncidentHelper incidentHelper;

    @Inject
    AssetCommunicationBean assetCommunication;

    @Inject
    @IncidentCreate
    private Event<Incident> createdIncident;

    @Inject
    @IncidentUpdate
    private Event<Incident> updatedIncident;

    @Inject
    private IncidentDao incidentDao;

    @Inject
    private IncidentLogDao incidentLogDao;

    @Inject
    private RiskCalculationsBean riskCalculationsBean;

    public OpenAndRecentlyResolvedIncidentsDto getAllOpenAndRecentlyResolvedIncidents() {
        OpenAndRecentlyResolvedIncidentsDto dto = new OpenAndRecentlyResolvedIncidentsDto();
        List<Incident> unresolvedIncidents = incidentDao.findOpenByTypes(Arrays.asList(IncidentType.values()));
        dto.setUnresolved(incidentHelper.incidentToDtoMap(unresolvedIncidents));

        List<Incident> resolvedSinceLast12Hours = incidentDao.findByStatusAndUpdatedSince12Hours(Arrays.asList(IncidentType.values()));
        dto.setRecentlyResolved(incidentHelper.incidentToDtoMap(resolvedSinceLast12Hours));
        return dto;
    }

    public void createIncident(IncidentTicketDto ticket) {
        if(ticket.getId() == null){
            upsertIncidentLackingTicketId(ticket);
        }else{
            internalCreateIncident(ticket);
        }

    }

    private void internalCreateIncident(IncidentTicketDto ticket){
        try {
            if(ticket.getType() == null){
                return;
            }

            if(IncidentType.ASSET_NOT_SENDING.equals(ticket.getType())) {
                String pollId = assetCommunication.createPollInternal(ticket);
                ticket.setPollId(pollId);
            }

            Incident incident = incidentHelper.constructIncident(ticket);
            incidentDao.save(incident);

            if ("Asset not sending".equalsIgnoreCase(ticket.getRuleGuid())) {
                RiskLevel riskLevel = riskCalculationsBean.calculateRiskLevelForIncident(incident);
                incident.setRisk(riskLevel);

                if(ticket.getPollId() != null && !ticket.getPollId().matches(uuidPattern)) {
                    incidentLogServiceBean.createIncidentLogForStatus(incident, "Creating autopoll failed since: " + ticket.getPollId(),
                            EventTypeEnum.AUTO_POLL_CREATED, null);
                } else {
                    incidentLogServiceBean.createIncidentLogForStatus(incident, "Asset not sending, sending autopoll",
                            EventTypeEnum.AUTO_POLL_CREATED, (ticket.getPollId() == null ? null : UUID.fromString(ticket.getPollId())));
                }
            } else {
                incidentLogServiceBean.createIncidentLogForStatus(incident, "Creating incident from rule " + ticket.getRuleGuid(),
                        EventTypeEnum.INCIDENT_CREATED, null);
            }

            createdIncident.fire(incident);
        }catch (IllegalArgumentException e) {
            LOG.error("Error: {} from UUID {}", e.getMessage(), ticket.getPollId());
            throw e;
        }
    }

    public IncidentDto createIncident(IncidentDto incidentDto, String user) {
        Incident incident = incidentHelper.incidentDtoToIncident(incidentDto);
        incident.setStatus(incidentDto.getType().equals(IncidentType.MANUAL_MODE) ? StatusEnum.MANUAL_POSITION_MODE : StatusEnum.INCIDENT_CREATED);
        incident.setCreateDate(Instant.now());
        Incident persistedIncident = incidentDao.save(incident);
        incidentLogServiceBean.createIncidentLogForStatus(persistedIncident, "Incident created by " + user, EventTypeEnum.INCIDENT_CREATED, null);
        createdIncident.fire(persistedIncident);
        return incidentHelper.incidentEntityToDto(persistedIncident);
    }

    public IncidentDto updateIncident(IncidentDto incidentDto, String user) {
        Incident incident = incidentDao.findById(incidentDto.getId());
        if(incident.getStatus().equals(StatusEnum.RESOLVED)){
            throw new IllegalArgumentException("Not allowed to update incident " + incident.getId() + " since it has status 'RESOLVED'");
        }
        EventTypeEnum eventType = mapEventType(incident, incidentDto);
        incidentHelper.populateIncident(incident, incidentDto);
        Incident updated = incidentDao.update(incident);
        updatedIncident.fire(updated);
        incidentLogServiceBean.createIncidentLogForStatus(updated, "Incident updated by " + user, eventType, null);
        return incidentHelper.incidentEntityToDto(incident);
    }

    private EventTypeEnum mapEventType(Incident incident, IncidentDto incidentDto) {
        if (!incident.getType().equals(incidentDto.getType())) {
            return EventTypeEnum.INCIDENT_TYPE;
        } else if (!incident.getStatus().toString().equals(incidentDto.getStatus())) {
            if(incidentDto.getStatus().equals(StatusEnum.RESOLVED)){
                return EventTypeEnum.INCIDENT_CLOSED;
            }
            return EventTypeEnum.INCIDENT_STATUS;
        }
        return EventTypeEnum.INCIDENT_UPDATED;
    }

    public void updateIncident(IncidentTicketDto ticket) {
        if(ticket.getId() == null){
            upsertIncidentLackingTicketId(ticket);
        }else {
            Incident persisted = incidentDao.findByTicketId(ticket.getId());
            internalUpdateIncident(ticket, persisted);
        }
    }

    private void internalUpdateIncident(IncidentTicketDto ticket, Incident persisted){

        if (persisted != null) {
            switch (persisted.getType()) {
                case ASSET_NOT_SENDING:
                    updateAssetNotSending(ticket, persisted);
                    break;
                case MANUAL_MODE:
                    updateManualMovement(ticket, persisted);
                    break;
                case PARKED:
                    updateParked(ticket, persisted);
                    break;
                case SEASONAL_FISHING:
                    updateSeasonalFishing(ticket, persisted);
                    break;
                case OWNER_TRANSFER:
                    updateOwnerTransfer(ticket, persisted);
                    break;
            }
            Incident updated = incidentDao.update(persisted);
            updatedIncident.fire(updated);

        }
    }

    private void updateAssetNotSending(IncidentTicketDto ticket, Incident persisted){
        if (ticket.getMovementId() != null &&
                !UUID.fromString(ticket.getMovementId()).equals(persisted.getMovementId())) {

            if (ticket.getMovementSource() != null && ticket.getMovementSource().equals(MovementSourceType.MANUAL)) {

                persisted.setStatus(StatusEnum.MANUAL_POSITION_MODE);
                persisted.setType(IncidentType.MANUAL_MODE);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, "Incident changed to type manual mode", EventTypeEnum.INCIDENT_TYPE, null);

                persisted.setMovementId(UUID.fromString(ticket.getMovementId()));
                incidentLogServiceBean.createIncidentLogForManualPosition(persisted, UUID.fromString(ticket.getMovementId()));
            } else if (ticket.getMovementSource() != null && !ticket.getMovementSource().equals(MovementSourceType.AIS)){
                persisted.setStatus(StatusEnum.RESOLVED);
                Incident updated = incidentDao.update(persisted);
                incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_CLOSED.getMessage(),
                        EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()));
            }
        }
    }

    private void updateManualMovement(IncidentTicketDto ticket, Incident persisted){
        if (ticket.getMovementSource().equals(MovementSourceType.MANUAL)
                && !incidentLogDao.checkIfMovementAlreadyExistsForIncident(persisted.getId(), UUID.fromString(ticket.getMovementId()))) {
            persisted.setMovementId(UUID.fromString(ticket.getMovementId()));
            incidentLogServiceBean.createIncidentLogForManualPosition(persisted, UUID.fromString(ticket.getMovementId()));
        } else if(ticket.getMovementSource().equals(MovementSourceType.AIS)){   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if(recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            }else{
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION.getMessage(), EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()));
            }
        } else {
            persisted.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
            incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION.getMessage(), EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()));
        }
    }

    private void updateParked(IncidentTicketDto ticket, Incident persisted){
        if(ticket.getMovementSource().equals(MovementSourceType.AIS)){   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if(recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            }else{
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION.getMessage(), EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()));
            }
        } else {
            persisted.setStatus(StatusEnum.RESOLVED);
            assetCommunication.setAssetParkedStatus(persisted.getAssetId(), false);
            incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION.getMessage(), EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()));
            incidentLogServiceBean.createIncidentLogForStatus(persisted, "Closing parked incident due to receiving VMS positions ", EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()));

        }
    }

    private void updateSeasonalFishing(IncidentTicketDto ticket, Incident persisted){
        if(ticket.getMovementSource().equals(MovementSourceType.AIS)){   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if(recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            }else{
                persisted.setStatus(StatusEnum.RECEIVING_AIS_POSITIONS);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION.getMessage(), EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()));
            }
        } else {
            persisted.setStatus(StatusEnum.RESOLVED);
            assetCommunication.setAssetParkedStatus(persisted.getAssetId(), false);
            incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION.getMessage(), EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()));
            incidentLogServiceBean.createIncidentLogForStatus(persisted, "Closing seasonal fishing incident due to receiving VMS positions ", EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()));
        }
    }

    private void updateOwnerTransfer(IncidentTicketDto ticket, Incident persisted){
        if(ticket.getMovementSource().equals(MovementSourceType.AIS)){   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if(recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            }else{
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION.getMessage(), EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()));
            }
        } else {
            incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION.getMessage(), EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()));
        }
    }


    public void addEventToIncident(long incidentId, EventCreationDto eventCreationDto) {
        Incident persisted = incidentDao.findById(incidentId);
        if(persisted.getStatus().equals(StatusEnum.RESOLVED)){
            throw new IllegalArgumentException("Not allowed to add event to incident " + incidentId + " since it has status 'RESOLVED'");
        }

        incidentLogServiceBean.createIncidentLogForStatus(persisted, eventCreationDto.getEventType().getMessage(),
                eventCreationDto.getEventType(), eventCreationDto.getRelatedObjectId());
    }

    public Incident findByTicketId(UUID ticketId) {
        return incidentDao.findByTicketId(ticketId);
    }

    public void upsertIncidentLackingTicketId(IncidentTicketDto ticketDto){
        List<Incident> openByAsset = incidentDao.findOpenByAsset(UUID.fromString(ticketDto.getAssetId()));
        if(openByAsset != null && !openByAsset.isEmpty()){
            if(ticketDto.getType() != null){
                return;
            }
            Incident openIncident = openByAsset.get(0);
            internalUpdateIncident(ticketDto, openIncident);
        }else{
            internalCreateIncident(ticketDto);
        }
    }
}
