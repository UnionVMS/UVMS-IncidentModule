package eu.europa.ec.fisheries.uvms.incident.mock;

import eu.europa.ec.fisheries.schema.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;
import eu.europa.ec.fisheries.uvms.rest.security.RequiresFeature;
import eu.europa.ec.fisheries.uvms.rest.security.UnionVMSFeature;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.bind.Jsonb;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("movement/rest/internal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class MovementMock {

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("getMicroMovement/{id}")
    public Response getMicroMovement(@PathParam("id") UUID id) {
        MicroMovement movement = new MicroMovement();
        movement.setId(id.toString());
        MovementPoint point = new MovementPoint();
        point.setLatitude(123d);
        point.setLongitude(123d);
        point.setAltitude(0d);
        movement.setLocation(point);

        movement.setSource(id.getMostSignificantBits() == 0l ? MovementSourceType.NAF : MovementSourceType.MANUAL);
        movement.setTimestamp(Instant.now());
        movement.setSpeed(122d);
        movement.setHeading(123d);
        String response = jsonb.toJson(movement);
        return Response.ok(response).build();
    }

    @POST
    @Path("/getMicroMovementList")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresFeature(UnionVMSFeature.manageInternalRest)
    public Response getMicroMovementByIdList(List<UUID> moveIds) {
        List<MicroMovement> responseList = new ArrayList<>();
        for (UUID uuid : moveIds) {
            MicroMovement movement = new MicroMovement();
            movement.setId(uuid.toString());
            MovementPoint point = new MovementPoint();
            point.setLatitude(123d);
            point.setLongitude(123d);
            point.setAltitude(0d);
            movement.setLocation(point);

            movement.setSource(uuid.getMostSignificantBits() == 0l ? MovementSourceType.NAF : MovementSourceType.MANUAL);
            movement.setTimestamp(Instant.now());
            movement.setSpeed(122d);
            movement.setHeading(123d);

            responseList.add(movement);
        }
        String response = jsonb.toJson(responseList);
        return Response.ok(response).build();
    }
}
