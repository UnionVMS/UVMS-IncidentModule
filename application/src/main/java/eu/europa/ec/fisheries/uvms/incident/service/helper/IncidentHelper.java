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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        }else{
            throw new IllegalArgumentException("Trying to create an incident for an assetGuid that does not exist. Guid: " + assetId);
        }
    }

    public Incident incidentDtoToIncident(IncidentDto incidentDto) {
        Incident incident = new Incident();
        return populateIncident(incident, incidentDto);
    }

    public Incident populateIncident(Incident incident, IncidentDto incidentDto) {
        incident.setAssetId(incidentDto.getAssetId());
        incident.setMobileTerminalId(incidentDto.getMobileTerminalId());
        incident.setTicketId(incidentDto.getTicketId());
        incident.setType(incidentDto.getType());
        incident.setAssetName(incidentDto.getAssetName());
        incident.setIrcs(incidentDto.getAssetIrcs());
        incident.setExpiryDate(incidentDto.getExpiryDate());
        if (incidentDto.getLastKnownLocation() != null) {
            incident.setMovementId(incidentDto.getLastKnownLocation().getId());
        }
        incident.setStatus(incidentDto.getStatus());
        return incident;
    }

    public IncidentDto incidentEntityToDto(Incident incident) {
        eu.europa.ec.fisheries.uvms.movement.model.dto.MovementDto movement = null;
        if(incident.getMovementId() != null) {
            movement = movementClient.getMovementById(incident.getMovementId());
            movement = movement.getId() != null ? movement : null ;    //for the sole reason that getMovementById returns an empty object if that move id does not exist
        }
        return mapEntityToDto(incident, movement);
    }

    public Map<Long,IncidentDto> incidentToDtoMap(List<Incident> incidentList) {
        List<UUID> movementIds = incidentList.stream()
                    .map(Incident::getMovementId)
                    .collect(Collectors.toList());
        Map<UUID, eu.europa.ec.fisheries.uvms.movement.model.dto.MovementDto> movementMap = movementClient.getMovementDtoByIdList(movementIds)
                    .stream()
                    .collect(Collectors.toMap(eu.europa.ec.fisheries.uvms.movement.model.dto.MovementDto::getId, Function.identity()));
        return incidentList
                    .stream()
                    .collect(Collectors.toMap(Incident::getId, i -> mapEntityToDto(i, movementMap.get(i.getMovementId()))));
    }
    
    private IncidentDto mapEntityToDto(Incident entity, eu.europa.ec.fisheries.uvms.movement.model.dto.MovementDto move) {
        IncidentDto dto = new IncidentDto();
        dto.setId(entity.getId());
        dto.setAssetId(entity.getAssetId());
        dto.setMobileTerminalId(entity.getMobileTerminalId());
        dto.setTicketId(entity.getTicketId());
        dto.setType(entity.getType());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetIrcs(entity.getIrcs());
        dto.setStatus(entity.getStatus());
        dto.setCreateDate(entity.getCreateDate());
        dto.setExpiryDate(entity.getExpiryDate());
        if (entity.getUpdateDate() != null) {
            dto.setUpdateDate(entity.getUpdateDate());
        }
        if(entity.getMovementId() != null && move != null) {
            dto.setLastKnownLocation(mapMovementMovementToIncidentMovement(move));
        }
        return dto;
    }

    private MovementDto mapMovementMovementToIncidentMovement(eu.europa.ec.fisheries.uvms.movement.model.dto.MovementDto move){
        MovementDto dto = new MovementDto();

        MovementPointDto location = new MovementPointDto();
        location.setLatitude(move.getLocation().getLatitude());
        location.setLongitude(move.getLocation().getLongitude());
        dto.setLocation(location);

        dto.setAisPositionAccuracy(move.getAisPositionAccuracy());
        dto.setAsset(move.getAsset());
        dto.setHeading(move.getHeading());
        dto.setId(move.getId());
        dto.setInternalReferenceNumber(move.getInternalReferenceNumber());
        dto.setLesReportTime(move.getLesReportTime());
        dto.setMovementType(move.getMovementType() != null ? move.getMovementType().value() : null);
        dto.setSource(MovementSourceType.fromValue(move.getSource().value()));
        dto.setSourceSatelliteId(move.getSourceSatelliteId() != null ? move.getSourceSatelliteId().name() : null);
        dto.setSpeed(move.getSpeed());
        dto.setStatus(move.getStatus());
        dto.setTimestamp(move.getTimestamp());
        dto.setTripNumber(move.getTripNumber());
        dto.setUpdated(move.getUpdated());
        dto.setUpdatedBy(move.getUpdatedBy());

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
