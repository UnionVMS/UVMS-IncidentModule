package eu.europa.ec.fisheries.uvms.incident.mock;

import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;

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
    public Response getMicroMovement(@PathParam("id") UUID id) {
        AssetDTO asset = new AssetDTO();
        asset.setId(id);
        asset.setName("Asset");
        asset.setIrcs("Ircs");

        return Response.ok(asset).build();
    }
}
