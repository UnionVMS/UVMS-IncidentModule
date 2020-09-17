/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.EventTypeEnum;
import eu.europa.ec.fisheries.uvms.incident.model.dto.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.ServiceConstants;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Startup
@Singleton
public class IncidentTimerBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentTimerBean.class);
    
    @Inject
    IncidentDao incidentDao;

    @Inject
    IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    @IncidentUpdate
    private Event<Incident> updatedIncident;

    @Schedule(minute = "*/1", hour = "*", persistent = false)
    public void manualPositionsTimer() {
        try {
            List<Incident> manualPositionIncidents = incidentDao.findByStatus(StatusEnum.MANUAL_POSITION_MODE);
            for (Incident incident : manualPositionIncidents) {
                if(incident.getExpiryDate().isBefore(Instant.now())){
                    incident.setStatus(StatusEnum.MANUAL_POSITION_LATE);
                    incidentLogServiceBean.createIncidentLogForStatus(incident, EventTypeEnum.MANUAL_POSITION_LATE.getMessage(), EventTypeEnum.MANUAL_POSITION_LATE, null);
                    updatedIncident.fire(incident);
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running manualPositionsTimer. ] {}", e);
        }
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void recentAisTimer() {
        try {
            List<Incident> incidentsThatReactOnRecentAIS = incidentDao.findOpenByTypes(ServiceConstants.REACT_ON_RECENT_AIS);
            for (Incident incident : incidentsThatReactOnRecentAIS) {
                if (incident.getStatus().equals(StatusEnum.RECEIVING_AIS_POSITIONS)) {
                    IncidentLog recentAisLog = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(incident.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
                    if (recentAisLog == null) {
                        incident.setStatus(StatusEnum.PARKED);
                        incidentLogServiceBean.createIncidentLogForStatus(incident, "Incident status updated to " + incident.getStatus(), EventTypeEnum.INCIDENT_STATUS, null);
                        updatedIncident.fire(incident);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running recentAisTimer. ] {}", e);
        }
    }

}
