package eu.europa.ec.fisheries.uvms.incident.rest;

import eu.europa.ec.fisheries.uvms.incident.arquillian.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.IncidentDto;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class TestResourceTest extends TransactionalTests {

    @Test
    public void assetNotSendingTest() {
        List<IncidentDto> response = getWebTarget()
                .path("incident/assetNotSending")
                .request(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<IncidentDto>>() {});
        assertNotNull(response);
    }
}
