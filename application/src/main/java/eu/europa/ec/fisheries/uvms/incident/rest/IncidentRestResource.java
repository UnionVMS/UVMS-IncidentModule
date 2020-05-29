/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */

package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentLogServiceBean;
import eu.europa.ec.fisheries.uvms.incident.service.bean.IncidentServiceBean;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentLogDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentLogDao;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.rest.security.RequiresFeature;
import eu.europa.ec.fisheries.uvms.rest.security.UnionVMSFeature;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("incident")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class IncidentRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentRestResource.class);

    @Inject
    private IncidentServiceBean incidentServiceBean;

    @Inject
    private IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    private IncidentHelper incidentHelper;

    @Inject
    private IncidentDao incidentDao;

    @Inject
    private IncidentLogDao incidentLogDao;

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @GET
    @Path("assetNotSending")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getAssetNotSendingEvents() {
        try {
            List<Incident> notSendingList = incidentServiceBean.getAssetNotSendingList();
            List<IncidentDto> dtoList = incidentHelper.incidentToDtoList(notSendingList);
            String response = jsonb.toJson(dtoList);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error while fetching AssetNotSending List", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @GET
    @Path("byTicketId/{ticketId}")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getByTicketId(@PathParam("ticketId") UUID ticketId) {
        try {
            Incident incident = incidentServiceBean.findByTicketId(ticketId);
            IncidentDto dto = incidentHelper.incidentEntityToDto(incident);
            String response = jsonb.toJson(dto);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error while fetching AssetNotSending List", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @GET
    @Path("incidentLogForIncident/{incidentId}")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getIncidentLogForIncident(@PathParam("incidentId") long incidentId) {
        try {
            List<IncidentLog> incidentLogs = incidentLogServiceBean.getIncidentLogByIncidentId(incidentId);
            List<IncidentLogDto> dtoList = incidentHelper.incidentLogToDtoList(incidentLogs);
            String response = jsonb.toJson(dtoList);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error while fetching incident log for incident {} ", incidentId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @GET
    @Path("incidentsForAssetId/{assetId}")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getIncidentsForAssetId(@PathParam("assetId") String assetId) {
        try {
            List<Incident> incidents = incidentDao.findByAssetId(UUID.fromString(assetId));
            List<IncidentDto> dtoList = incidentHelper.incidentToDtoList(incidents);
            String response = jsonb.toJson(dtoList);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error while fetching incidents for asset id {} ", assetId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @GET
    @Path("incidentLogsForAssetId/{assetId}")
    @RequiresFeature(UnionVMSFeature.viewAlarmsOpenTickets)
    public Response getIncidentLogsForAsset(@PathParam("assetId") String assetId) {
        try {
            List<Incident> incidents = incidentDao.findByAssetId(UUID.fromString(assetId));
            List<Long> incidentIdList = incidents.stream().map(Incident::getId).collect(Collectors.toList());
            List<IncidentLog> incidentLogs = new ArrayList<>();
            if(!incidentIdList.isEmpty()) {
                incidentLogs = incidentLogDao.findAllByIncidentId(incidentIdList);
            }
            List<IncidentLogDto> dtoList = incidentHelper.incidentLogToDtoList(incidentLogs);
            String response = jsonb.toJson(dtoList);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error while fetching incidents for asset id {} ", assetId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

    @POST
    @Path("updateStatusForIncident/{incidentId}")
    @RequiresFeature(UnionVMSFeature.manageAlarmsOpenTickets)
    public Response updateIncident(@PathParam("incidentId") long incidentId, StatusDto status) {
        try {
            Incident updated = incidentServiceBean.updateIncidentStatus(incidentId, status);
            IncidentDto dto = incidentHelper.incidentEntityToDto(updated);
            String response = jsonb.toJson(dto);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error while fetching AssetNotSending List", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getRootCause(e)).build();
        }
    }

}
