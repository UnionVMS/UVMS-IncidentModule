package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.AssetNotSendingDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.ServiceConstants;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentCreate;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentUpdate;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.movement.client.MovementRestClient;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentServiceBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentServiceBean.class);

    @Inject
    private IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    private IncidentHelper incidentHelper;

    @EJB
    private MovementRestClient movementClient;

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

    public AssetNotSendingDto getAssetNotSendingList() {
        AssetNotSendingDto dto = new AssetNotSendingDto();
        List<Incident> unresolvedIncidents = incidentDao.findUnresolvedIncidents(IncidentType.ASSET_NOT_SENDING);
        dto.setUnresolved(incidentHelper.incidentToDtoMap(unresolvedIncidents));

        List<Incident> resolvedSinceLast12Hours = incidentDao.findByStatusAndUpdatedSince12Hours(IncidentType.ASSET_NOT_SENDING);
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
            Incident incident = incidentHelper.constructIncident(ticket);
            incidentDao.save(incident);

            if ("Asset not sending".equalsIgnoreCase(ticket.getRuleGuid())) {

                if(ticket.getPollId() != null && ticket.getPollId().length() != 36 ) {
                    incidentLogServiceBean.createIncidentLogForStatus(incident, "Creating autopoll failed since pollId is: " + ticket.getPollId(),
                            EventTypeEnum.AUTO_POLL_CREATED, null);
                } else {
                    incidentLogServiceBean.createIncidentLogForStatus(incident, "Asset not sending, sending autopoll",
                            EventTypeEnum.AUTO_POLL_CREATED, (ticket.getPollId() == null ? null : UUID.fromString(ticket.getPollId())));
                }
            } else {
                incidentLogServiceBean.createIncidentLogForStatus(incident, "Creating incident from rule " + ticket.getRuleGuid(),
                        EventTypeEnum.INCIDENT_STATUS, null);
            }

            createdIncident.fire(incident);
        }catch (IllegalArgumentException e) {
            LOG.error("Error: {} from UUID {}", e.getMessage(), ticket.getPollId());
            throw e;
        }
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

            if (ticket.getStatus().equals(TicketStatusType.CLOSED.value())) {
                persisted.setStatus(StatusEnum.RESOLVED);
                Incident updated = incidentDao.update(persisted);
                updatedIncident.fire(updated);
                incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_CLOSED.getMessage(),
                        EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()));

            } else if (ticket.getMovementId() != null &&
                    !UUID.fromString(ticket.getMovementId()).equals(persisted.getMovementId())) {
                MicroMovement movementFromTicketUpdate = movementClient.getMicroMovementById(UUID.fromString(ticket.getMovementId()));

                if (movementFromTicketUpdate != null && movementFromTicketUpdate.getSource().equals(MovementSourceType.MANUAL)) {
                    if (!incidentLogDao.checkIfMovementAlreadyExistsForIncident(persisted.getId(), UUID.fromString(ticket.getMovementId()))){
                        persisted.setStatus(StatusEnum.MANUAL_POSITION_MODE);
                        persisted.setMovementId(UUID.fromString(ticket.getMovementId()));
                        incidentLogServiceBean.createIncidentLogForManualPosition(persisted, movementFromTicketUpdate);
                    }
                } else {
                    persisted.setStatus(StatusEnum.INCIDENT_AUTO_UPDATED);
                    incidentLogServiceBean.createIncidentLogForStatus(persisted, "Update from rule: " + ticket.getRuleGuid(), EventTypeEnum.INCIDENT_STATUS, null);
                }
                Incident updated = incidentDao.update(persisted);
                updatedIncident.fire(updated);
            }
        }
    }

    public Incident updateIncidentStatus(long incidentId, StatusDto statusDto, String user) {
        Incident persisted = incidentDao.findById(incidentId);
        if(ServiceConstants.RESOLVED_STATUS_LIST.contains(persisted.getStatus())){
            persisted.setStatus(StatusEnum.REOPEN);
            incidentLogServiceBean.createIncidentLogForStatus(persisted, "Incident reopened by: " + user + ". Incident can not autoclose.",
                    EventTypeEnum.INCIDENT_STATUS, null);
        }
        persisted.setStatus(statusDto.getStatus());
        Incident updated = incidentDao.update(persisted);
        updatedIncident.fire(updated);
        incidentLogServiceBean.createIncidentLogForStatus(updated, statusDto.getEventType().getMessage(),
                statusDto.getEventType(), statusDto.getRelatedObjectId());
        return updated;
    }

    public Incident findByTicketId(UUID ticketId) {
        return incidentDao.findByTicketId(ticketId);
    }

    public void upsertIncidentLackingTicketId(IncidentTicketDto ticketDto){
        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(UUID.fromString(ticketDto.getAssetId()), ticketDto.getType());
        if(openByAssetAndType != null){
            internalUpdateIncident(ticketDto, openByAssetAndType);
        }else{
            internalCreateIncident(ticketDto);
        }
    }
}
