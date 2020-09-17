package eu.europa.ec.fisheries.uvms.incident.service;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.IncidentType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;

import java.util.Arrays;
import java.util.List;

public class ServiceConstants {

    public static final int MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES = 65;

    public static final List<StatusEnum> RESOLVED_STATUS_LIST = Arrays.asList(StatusEnum.RESOLVED);
    public static final List<IncidentType> REACT_ON_RECENT_AIS = Arrays.asList(IncidentType.MANUAL_POSITION_MODE, IncidentType.OWNERSHIP_TRANSFER, IncidentType.PARKED, IncidentType.SEASONAL_FISHING);
}
