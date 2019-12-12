/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */

package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentLogServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.IncidentLogDto;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.rest.security.RequiresFeature;
import eu.europa.ec.fisheries.uvms.rest.security.UnionVMSFeature;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("incident")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class IncidentResource {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentResource.class);

    @Inject
    private IncidentServiceBean incidentServiceBean;

    @Inject
    private IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    private IncidentHelper incidentHelper;

    @GET
    @Path("assetNotSending")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getAssetNotSendingEvents() {
        try {
            List<Incident> notSendingList = incidentServiceBean.getAssetNotSendingList();
            List<IncidentDto> dtoList = incidentHelper.incidentToDtoList(notSendingList);
            return Response.ok(dtoList).build();
        } catch (Exception e) {
            LOG.error("Error while fetching AssetNotSending List", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @GET
    @Path("assetNotSendingChanges/{incidentId}")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getAssetNotSendingEventChanges(@PathParam("incidentId") long incidentId) {
        try {
            List<IncidentLog> eventChanges = incidentLogServiceBean.getAssetNotSendingEventChanges(incidentId);
            List<IncidentLogDto> dtoList = incidentHelper.incidentLogToDtoList(eventChanges);
            return Response.ok(dtoList).build();
        } catch (Exception e) {
            LOG.error("Error while fetching AssetNotSending List", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @POST
    @Path("assetNotSending/{incidentId}/status")
    @RequiresFeature(UnionVMSFeature.manageAlarmsOpenTickets)
    public Response updateAssetNotSendingStatus(@PathParam("incidentId") long incidentId, StatusDto status) {
        try {
            Incident updated = incidentServiceBean.updateIncidentStatus(incidentId, status);
            IncidentDto dto = incidentHelper.incidentEntityToDto(updated);
            return Response.ok(dto).build();
        } catch (Exception e) {
            LOG.error("Error while fetching AssetNotSending List", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }
}