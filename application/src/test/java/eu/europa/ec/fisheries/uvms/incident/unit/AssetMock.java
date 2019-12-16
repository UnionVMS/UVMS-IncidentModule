package eu.europa.ec.fisheries.uvms.incident.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europa.ec.fisheries.schema.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("asset/rest/internal")
@Stateless
public class AssetMock {

    @GET
    @Path("asset/guid/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMicroMovement(@PathParam("id") UUID id) throws JsonProcessingException {
        AssetDTO asset = new AssetDTO();
        asset.setId(id);
        asset.setName("Asset");
        asset.setIrcs("Ircs");

        return Response.ok(asset).build();
    }
}
