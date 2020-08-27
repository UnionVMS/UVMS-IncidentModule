package eu.europa.ec.fisheries.uvms.incident.service.helper;

import eu.europa.ec.fisheries.uvms.asset.client.AssetClient;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetIdentifier;
import eu.europa.ec.fisheries.uvms.incident.model.dto.*;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.MovementSourceType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.movement.client.MovementRestClient;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Stateless
public class IncidentHelper {

    @EJB
    private AssetClient assetClient;

    @EJB
    private MovementRestClient movementClient;

    public Incident constructIncident(IncidentTicketDto ticket) {
        Incident incident = new Incident();
        if (ticket.getMobTermId() != null) {
            incident.setMobileTerminalId(UUID.fromString(ticket.getMobTermId()));
        }
        incident.setCreateDate(Instant.now());
        incident.setStatus(StatusEnum.INCIDENT_CREATED);
        incident.setTicketId(ticket.getId());
        incident.setType(ticket.getType());
        incident.setMovementId(ticket.getMovementId() != null ? UUID.fromString(ticket.getMovementId()) : null);
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

    public Incident incidentDtoToIncident(IncidentDto incidentDto) {
        Incident incident = new Incident();
        incident.setAssetId(incidentDto.getAssetId());
        incident.setMobileTerminalId(incidentDto.getMobileTerminalId());
        incident.setTicketId(incidentDto.getTicketId());
        incident.setType(incidentDto.getType());
        incident.setAssetName(incidentDto.getAssetName());
        incident.setIrcs(incidentDto.getAssetIrcs());
        if (incidentDto.getLastKnownLocation() != null) {
            incident.setMovementId(UUID.fromString(incidentDto.getLastKnownLocation().getId()));
        }
        if (incidentDto.getStatus() != null) {
            incident.setStatus(StatusEnum.valueOf(incidentDto.getStatus()));
        }
        return incident;
    }

    public IncidentDto incidentEntityToDto(Incident incident) {
        MicroMovement movement = null;
        if(incident.getMovementId() != null) {
            movement = movementClient.getMicroMovementById(incident.getMovementId());
        }
        return mapEntityToDto(incident, movement);
    }

    public Map<Long,IncidentDto> incidentToDtoMap(List<Incident> incidentList) {
        List<UUID> movementIds = incidentList.stream()
                    .map(Incident::getMovementId)
                    .collect(Collectors.toList());
        Map<String, MicroMovement> movementMap = movementClient.getMicroMovementByIdList(movementIds)
                    .stream()
                    .collect(Collectors.toMap(MicroMovement::getId, Function.identity()));
        return incidentList
                    .stream()
                    .collect(Collectors.toMap(Incident::getId, i -> mapEntityToDto(i, movementMap.get(i.getMovementId().toString()))));
    }
    
    private IncidentDto mapEntityToDto(Incident entity, MicroMovement micro) {
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
        dto.setExpiryDate(entity.getExpiryDate());
        if (entity.getUpdateDate() != null) {
            dto.setUpdateDate(entity.getUpdateDate());
        }
        if(entity.getMovementId() != null && micro != null) {
            MicroMovementDto lastKnownLocation = new MicroMovementDto();

            MovementPointDto location = new MovementPointDto();
            location.setLatitude(micro.getLocation().getLatitude());
            location.setLongitude(micro.getLocation().getLongitude());
            if (micro.getLocation().getAltitude() != null) {
                location.setAltitude(micro.getLocation().getAltitude());
            }
            lastKnownLocation.setLocation(location);
            lastKnownLocation.setHeading(micro.getHeading());
            lastKnownLocation.setId(micro.getId());
            lastKnownLocation.setTimestamp(micro.getTimestamp());
            lastKnownLocation.setSpeed(micro.getSpeed());
            lastKnownLocation.setSource(MovementSourceType.fromValue(micro.getSource().name()));

            dto.setLastKnownLocation(lastKnownLocation);
        }
        return dto;
    }

    public Map<Long, IncidentLogDto> incidentLogToDtoMap(List<IncidentLog> incidentLogList) {
        Map<Long, IncidentLogDto> retVal = new HashMap<>(incidentLogList.size());
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
            retVal.put(dto.getId(), dto);
        }
        return retVal;
    }
}
