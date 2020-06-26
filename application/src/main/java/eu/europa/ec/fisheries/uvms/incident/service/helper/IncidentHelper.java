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
import java.util.*;
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

    public IncidentDto incidentEntityToDto(Incident incident) {
        return mapEntityToDto(incident);
    }

//    public Map<Long, IncidentDto> incidentToDtoMap(List<Incident> incidentList) {
//        Map<Long, IncidentDto> retVal = new TreeMap<>();
//        for (Incident i : incidentList) {
//            IncidentDto dto = mapEntityToDto(i);
//            retVal.put(dto.getId(), dto);
//        }
//        return retVal;
//    }
    
  public Map<Long,IncidentDto> incidentToDtoMap(List<Incident> incidentList) {
      
      List<UUID> listOfMoveIds = new ArrayList<>();
      for (Incident i : incidentList) {
          listOfMoveIds.add(i.getMovementId());
      }
      
      List<MicroMovement> microList = movementClient.getMicroMovementByIdList(listOfMoveIds);
      
      Map<Long, IncidentDto> retVal = new HashMap<>();//TreeMap<>();
      for (Incident i : incidentList) {
          MicroMovement m = null;
          for (MicroMovement micro : microList) {
              if(i.getMovementId().toString().equals(micro.getId())) {
                  m = micro;
              }
          }
          IncidentDto dto = mapEntityToDto(i, m);
          retVal.put(dto.getId(), dto);
      }
      return retVal;
      
  }   
    
//    public Map<Long,IncidentDto> incidentToDtoMap(List<Incident> incidentList) {
//        
//        List<UUID> listOfMoveIds = incidentList.stream()
//                    .map(i -> i.getMovementId())
//                    .collect(Collectors.toList());
//        List<MicroMovement> microList = movementClient.getMicroMovementByIdList(listOfMoveIds);
//        Map<String, MicroMovement> microMap =
//                microList.stream().collect(Collectors.toMap(MicroMovement::getId,
//                        m -> m));
//        return incidentList.stream()
//                .collect(Collectors.toMap(Incident::getId, 
//                        i -> mapEntityToDto(i, microMap.get(i.getMovementId().toString()))));
//    }
    
    private IncidentDto mapEntityToDto(Incident entity, MicroMovement micro) {
        IncidentDto dto = new IncidentDto();
        dto.setId(entity.getId());
        dto.setAssetId(entity.getAssetId());
        dto.setMobileTerminalId(entity.getMobileTerminalId());
        dto.setTicketId(entity.getTicketId());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetIrcs(entity.getIrcs());
        dto.setStatus(entity.getStatus().name());
        dto.setCreateDate(entity.getCreateDate());
        if (entity.getUpdateDate() != null) {
            dto.setUpdateDate(entity.getUpdateDate());
        }
        if(entity.getMovementId() != null && micro != null) {
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
        return dto;
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

    public Map<Long, IncidentLogDto> incidentLogToDtoMap(List<IncidentLog> incidentLogList) {
        Map<Long, IncidentLogDto> retVal = new TreeMap<>();
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
