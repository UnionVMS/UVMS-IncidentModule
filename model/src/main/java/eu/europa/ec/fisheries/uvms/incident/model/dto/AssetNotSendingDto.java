package eu.europa.ec.fisheries.uvms.incident.model.dto;

import java.util.List;

public class AssetNotSendingDto {

    List<IncidentDto> unresolved;

    List<IncidentDto> recentlyResolved;

    public List<IncidentDto> getUnresolved() {
        return unresolved;
    }

    public void setUnresolved(List<IncidentDto> unresolved) {
        this.unresolved = unresolved;
    }

    public List<IncidentDto> getRecentlyResolved() {
        return recentlyResolved;
    }

    public void setRecentlyResolved(List<IncidentDto> recentlyResolved) {
        this.recentlyResolved = recentlyResolved;
    }
}
