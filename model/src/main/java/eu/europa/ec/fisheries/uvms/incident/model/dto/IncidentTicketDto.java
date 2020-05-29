package eu.europa.ec.fisheries.uvms.incident.model.dto;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class IncidentTicketDto implements Serializable {

    private TicketType type;
    private UUID id;
    private String assetId;
    private String mobTermId;
    private String channelId;
    private String movementId;
    private String pollId;

    private String ruleGuid;
    private String ruleName;
    private String recipient;
    private String status;
    private Long ticketCount;

    private Instant createdDate;
    private Instant updated;
    private String updatedBy;

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getMobTermId() {
        return mobTermId;
    }

    public void setMobTermId(String mobTermId) {
        this.mobTermId = mobTermId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getMovementId() {
        return movementId;
    }

    public void setMovementId(String movementId) {
        this.movementId = movementId;
    }

    public String getPollId() {
        return pollId;
    }

    public void setPollId(String pollId) {
        this.pollId = pollId;
    }

    public String getRuleGuid() {
        return ruleGuid;
    }

    public void setRuleGuid(String ruleGuid) {
        this.ruleGuid = ruleGuid;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Long ticketCount) {
        this.ticketCount = ticketCount;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
