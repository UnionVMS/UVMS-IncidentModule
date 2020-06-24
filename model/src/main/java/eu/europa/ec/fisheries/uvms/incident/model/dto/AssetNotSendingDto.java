package eu.europa.ec.fisheries.uvms.incident.model.dto;

import java.util.Map;

public class AssetNotSendingDto {

    Map<Long, IncidentDto> unresolved;

    Map<Long, IncidentDto> recentlyResolved;

    public Map<Long, IncidentDto> getUnresolved() {
        return unresolved;
    }

    public void setUnresolved(Map<Long, IncidentDto> unresolved) {
        this.unresolved = unresolved;
    }

    public Map<Long, IncidentDto> getRecentlyResolved() {
        return recentlyResolved;
    }

    public void setRecentlyResolved(Map<Long, IncidentDto> recentlyResolved) {
        this.recentlyResolved = recentlyResolved;
    }
}
