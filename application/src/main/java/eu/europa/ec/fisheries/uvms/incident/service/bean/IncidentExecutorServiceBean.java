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
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Startup
@Singleton
public class IncidentExecutorServiceBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentExecutorServiceBean.class);
    
    @Inject
    IncidentDao incidentDao;

    @Inject
    IncidentLogServiceBean incidentLogServiceBean;

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void manualPositionsTimer() {
        try {
            List<Incident> manualPositionIncidents = incidentDao.findByStatus(StatusEnum.MANUAL_POSITION_MODE);
            for (Incident incident : manualPositionIncidents) {
                if(incident.getUpdateDate().plus(65, ChronoUnit.MINUTES).isBefore(Instant.now())){
                    incident.setStatus(StatusEnum.MANUAL_POSITION_LATE);
                    incidentLogServiceBean.createIncidentLogForStatus(incident, EventTypeEnum.MANUAL_POSITION_LATE.getMessage(), EventTypeEnum.MANUAL_POSITION_LATE, null);
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running manualPositionsTimer. ] {}", e);
        }
    }

}
