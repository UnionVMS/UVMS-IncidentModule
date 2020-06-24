package eu.europa.ec.fisheries.uvms.incident.service.helper;

import eu.europa.ec.fisheries.uvms.asset.client.AssetClient;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetIdentifier;
import eu.europa.ec.fisheries.uvms.incident.model.dto.*;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.MovementSourceType;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.movement.client.MovementRestClient;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentHelper {

    @EJB
    private AssetClient assetClient;

    @EJB
    private MovementRestClient movementClient;

    public Incident constructIncident(IncidentTicketDto ticket, MicroMovement movement) {
        Incident incident = new Incident();
        if (ticket.getMobTermId() != null) {
            incident.setMobileTerminalId(UUID.fromString(ticket.getMobTermId()));
        }
        incident.setCreateDate(Instant.now());
        incident.setStatus(StatusEnum.INCIDENT_CREATED);
        incident.setTicketId(ticket.getId());
        incident.setType(ticket.getType());
        if (movement != null) {
            incident.setMovementId(UUID.fromString(movement.getId()));
        }
        setAssetValues(incident, ticket.getAssetId());
        return incident;
    }

    private void setAssetValues(Incident incident, String assetId) {
        AssetDTO asset = assetClient.getAssetById(AssetIdentifier.GUID, assetId);
        if(asset != null) {
            incident.setAssetId(asset.getId());
            incident.setAssetName(asset.getName());
            incident.setIrcs(asset.getIrcs());
        }
    }

    public IncidentDto incidentEntityToDto(Incident incident) {
        return mapEntityToDto(incident);
    }

    public List<IncidentDto> incidentToDtoList(List<Incident> incidentList) {
        List<IncidentDto> retVal = new ArrayList<>();
        for (Incident i : incidentList) {
            IncidentDto dto = mapEntityToDto(i);
            retVal.add(dto);
        }
        return retVal;
    }

    private IncidentDto mapEntityToDto(Incident entity) {
        IncidentDto dto = new IncidentDto();
        dto.setId(entity.getId());
        dto.setAssetId(entity.getAssetId());
        dto.setMobileTerminalId(entity.getMobileTerminalId());
        dto.setTicketId(entity.getTicketId());
        dto.setType(entity.getType());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetIrcs(entity.getIrcs());
        dto.setStatus(entity.getStatus().name());
        dto.setCreateDate(entity.getCreateDate());
        if (entity.getUpdateDate() != null)
            dto.setUpdateDate(entity.getUpdateDate());

        if(entity.getMovementId() != null) {
            MicroMovement micro = movementClient.getMicroMovementById(entity.getMovementId());
            if(micro != null) {
                MicroMovementDto lastKnownLocation = new MicroMovementDto();

                MovementPointDto location = new MovementPointDto();
                location.setLatitude(micro.getLocation().getLatitude());
                location.setLongitude(micro.getLocation().getLongitude());
                if (micro.getLocation().getAltitude() != null)
                    location.setAltitude(micro.getLocation().getAltitude());

                lastKnownLocation.setLocation(location);
                lastKnownLocation.setHeading(micro.getHeading());
                lastKnownLocation.setId(micro.getId());
                lastKnownLocation.setTimestamp(micro.getTimestamp());
                lastKnownLocation.setSpeed(micro.getSpeed());
                lastKnownLocation.setSource(MovementSourceType.fromValue(micro.getSource().name()));

                dto.setLastKnownLocation(lastKnownLocation);
            }
        }
        return dto;
    }

    public List<IncidentLogDto> incidentLogToDtoList(List<IncidentLog> incidentLogList) {
        List<IncidentLogDto> retVal = new ArrayList<>();
        for (IncidentLog entity : incidentLogList) {
            IncidentLogDto dto = new IncidentLogDto();
            dto.setId(entity.getId());
            dto.setIncidentId(entity.getIncidentId());
            dto.setMessage(entity.getMessage());
            dto.setEventType(entity.getEventType());
            dto.setCreateDate(entity.getCreateDate());
            dto.setRelatedObjectId(entity.getRelatedObjectId());
            dto.setRelatedObjectType(entity.getRelatedObjectType());
            dto.setIncidentStatus(entity.getIncidentStatus());
            retVal.add(dto);
        }
        return retVal;
    }
}
