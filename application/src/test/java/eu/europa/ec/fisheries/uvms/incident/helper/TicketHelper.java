package eu.europa.ec.fisheries.uvms.incident.helper;

import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;

import java.time.Instant;
import java.util.UUID;

public class TicketHelper {

    public static IncidentTicketDto createTicket(UUID assetId, UUID movId, UUID mobTermId) {
        IncidentTicketDto ticket = new IncidentTicketDto();
        ticket.setType(null);
        ticket.setAssetId(assetId.toString());
        ticket.setMovementId(movId.toString());
        ticket.setMobTermId(mobTermId.toString());
        ticket.setRuleName("Asset not sending");
        ticket.setRuleGuid("Asset not sending");
        ticket.setUpdatedBy("UVMS");
        ticket.setStatus(TicketStatusType.POLL_PENDING.value());
        ticket.setTicketCount(1L);
        Instant date = Instant.now();
        ticket.setCreatedDate(date);
        ticket.setUpdated(date);
        ticket.setPositionTime(date);
        return ticket;
    }

    public static IncidentDto createBasicIncidentDto() {
        IncidentDto incidentDto = new IncidentDto();
        incidentDto.setAssetId(UUID.randomUUID());
        incidentDto.setAssetName("Test asset");
        incidentDto.setStatus(StatusEnum.PARKED);
        incidentDto.setType(IncidentType.PARKED);
        return incidentDto;
    }
}
