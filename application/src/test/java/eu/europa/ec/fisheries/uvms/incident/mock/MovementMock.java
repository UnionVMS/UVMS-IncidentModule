package eu.europa.ec.fisheries.uvms.incident.mock;

import eu.europa.ec.fisheries.schema.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.bind.Jsonb;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;

@Path("movement/rest/internal")
@Stateless
public class MovementMock {

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @GET
    @Path("getMicroMovement/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMicroMovement(@PathParam("id") UUID id) {
        MicroMovement movement = new MicroMovement();
        movement.setGuid(id.toString());
        MovementPoint point = new MovementPoint();
        point.setLatitude(123d);
        point.setLongitude(123d);
        point.setAltitude(0d);
        movement.setLocation(point);
        movement.setSource(MovementSourceType.MANUAL);
        movement.setTimestamp(Instant.now());
        movement.setSpeed(122d);
        movement.setHeading(123d);
        String response = jsonb.toJson(movement);
        return Response.ok(response).build();
    }
}
