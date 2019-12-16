package eu.europa.ec.fisheries.uvms.incident;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.europa.ec.fisheries.schema.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;

@Path("movement/rest/internal")
@Stateless
public class MovementMock {

    private ObjectMapper objectMapper = new ObjectMapper();

    @GET
    @Path("getMicroMovement/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMicroMovement(@PathParam("id") UUID id) throws JsonProcessingException {

        objectMapper
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules();

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
        String response = objectMapper.writeValueAsString(movement);
        return Response.ok(response).build();
    }
}
