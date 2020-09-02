package eu.europa.ec.fisheries.uvms.incident.mock;

import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;

import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("asset/rest/internal")
@Consumes(value = { MediaType.APPLICATION_JSON })
@Produces(value = { MediaType.APPLICATION_JSON })
@Stateless
public class AssetMock {

    @GET
    @Path("asset/guid/{id}")
    public Response getMicroMovement(@PathParam("id") UUID id) {
        AssetDTO asset = new AssetDTO();
        asset.setId(id);
        asset.setName("Asset");
        asset.setIrcs("Ircs");

        return Response.ok(asset).build();
    }

    @POST
    @Path("/createPollForAsset/{id}")
    public Response createPoll(@PathParam("id") String assetId, @QueryParam("username") String username, @QueryParam("comment") String comment) {
        System.setProperty("AssetPollEndpointReached", "True");
        return Response.ok().entity(Boolean.TRUE).build();
    }
}
