package eu.europa.ec.fisheries.uvms.incident.rest;

import static org.junit.Assert.assertThat;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;

@RunWith(Arquillian.class)
public class TestResourceTest extends TransactionalTests {

    @Test
    public void test() {
        String response = ClientBuilder.newClient()
            .target("http://localhost:8080/incident/rest/hello")
            .request(MediaType.APPLICATION_JSON)
            .get(String.class);
        assertThat(response, CoreMatchers.is("hello"));
    }
}
