package eu.europa.ec.fisheries.uvms.incident.helper;

import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;

import javax.ejb.Stateless;
import java.time.Instant;
import java.util.UUID;

@Stateless
public class TicketHelper {

    public TicketType createTicket(UUID ticketId, UUID assetId, UUID movId, UUID mobTermId) {
        TicketType ticket = new TicketType();
        ticket.setGuid(ticketId.toString());
        ticket.setAssetGuid(assetId.toString());
        ticket.setMovementGuid(movId.toString());
        ticket.setMobileTerminalGuid(mobTermId.toString());
        ticket.setRuleName("Asset not sending");
        ticket.setRuleGuid("Asset not sending");
        ticket.setUpdatedBy("UVMS");
        ticket.setStatus(TicketStatusType.POLL_PENDING);
        ticket.setTicketCount(1L);
        String date = String.valueOf(Instant.now());
        ticket.setOpenDate(date);
        ticket.setUpdated(date);
        return ticket;
    }
}
