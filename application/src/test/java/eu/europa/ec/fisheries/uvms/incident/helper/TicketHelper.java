package eu.europa.ec.fisheries.uvms.incident.helper;

import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import java.time.Instant;
import java.util.UUID;

public class TicketHelper {

    public static IncidentTicketDto createTicket(UUID ticketId, UUID assetId, UUID movId, UUID mobTermId) {
        IncidentTicketDto ticket = new IncidentTicketDto();
        ticket.setId(ticketId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
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
        return ticket;
    }

    public static IncidentDto createIncidentDto() {
        IncidentDto incidentDto = new IncidentDto();
        incidentDto.setAssetId(UUID.randomUUID());
        incidentDto.setAssetName("Test asset");
        incidentDto.setStatus("INCIDENT_CREATED");
        incidentDto.setType(IncidentType.LONG_TERM_PARKED);
        return incidentDto;
    }
}
