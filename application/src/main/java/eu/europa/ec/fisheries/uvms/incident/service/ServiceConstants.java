package eu.europa.ec.fisheries.uvms.incident.service;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;

import java.util.Arrays;
import java.util.List;

public class ServiceConstants {

    public static final List<StatusEnum> RESOLVED_STATUS_LIST = Arrays.asList(StatusEnum.LONG_TERM_PARKED, StatusEnum.RESOLVED);
}
