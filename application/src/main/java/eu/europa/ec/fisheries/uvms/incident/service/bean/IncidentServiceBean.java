package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentCreate;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentUpdate;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.movement.client.MovementRestClient;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentServiceBean {

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

    public List<Incident> getAssetNotSendingList() {
        List<Incident> unresolvedIncidents = incidentDao.findUnresolvedIncidents();
        List<Incident> resolvedSinceLast12Hours = incidentDao.findByStatusAndUpdatedSince();
        unresolvedIncidents.addAll(resolvedSinceLast12Hours);
        return unresolvedIncidents;
    }

    public void createIncident(IncidentTicketDto ticket) {
        MicroMovement movement = movementClient.getMicroMovementById(UUID.fromString(ticket.getMovementId()));
        if ("Asset not sending".equalsIgnoreCase(ticket.getRuleGuid())) {
            Incident incident = incidentHelper.constructIncident(ticket, movement);
            incidentDao.save(incident);

            incidentLogServiceBean.createIncidentLogForStatus(incident, "Asset not sending, sending autopoll",
                    EventTypeEnum.POLL_CREATED, UUID.fromString(ticket.getPollId()));
            createdIncident.fire(incident);
        }
    }

    public void updateIncident(IncidentTicketDto ticket) {
        Incident persisted = incidentDao.findByTicketId(ticket.getId());

        if (persisted != null) {
            String incidentStatus = persisted.getStatus().name();

            if (ticket.getStatus().equals(TicketStatusType.CLOSED.value())) {
                persisted.setStatus(StatusEnum.RESOLVED);
                Incident updated = incidentDao.update(persisted);
                updatedIncident.fire(updated);
                incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_CLOSED.getMessage(),
                        EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()));

            } else if (ticket.getMovementId() != null &&
                    !ticket.getMovementId().equals(persisted.getMovementId().toString())) {
                MicroMovement movementFromTicketUpdate = movementClient.getMicroMovementById(UUID.fromString(ticket.getMovementId()));

                if (movementFromTicketUpdate != null && movementFromTicketUpdate.getSource().equals(MovementSourceType.MANUAL)) {
                    persisted.setStatus(StatusEnum.MANUAL_POSITION_MODE);
                    Incident updated = incidentDao.update(persisted);
                    updatedIncident.fire(updated);
                    incidentLogServiceBean.createIncidentLogForManualPosition(persisted, movementFromTicketUpdate);
                }
            }
        }
    }

    public Incident updateIncidentStatus(long incidentId, StatusDto statusDto) {
        Incident persisted = incidentDao.findById(incidentId);
        persisted.setStatus(statusDto.getStatus());
        Incident updated = incidentDao.update(persisted);
        updatedIncident.fire(updated);
        incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_STATUS.getMessage(),
                statusDto.getEventType(), statusDto.getRelatedObjectId());
        return updated;
    }

    public Incident findByTicketId(UUID ticketId) {
        return incidentDao.findByTicketId(ticketId);
    }
}
