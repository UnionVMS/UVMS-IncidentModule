package eu.europa.ec.fisheries.uvms.incident;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.mock.AssetMock;
import eu.europa.ec.fisheries.uvms.incident.mock.MovementMock;
import eu.europa.ec.fisheries.uvms.incident.mock.SpatialModuleMock;
import eu.europa.ec.fisheries.uvms.incident.mock.UnionVMSRestMock;
import eu.europa.ec.fisheries.uvms.rest.security.UnionVMSFeature;
import eu.europa.ec.mare.usm.jwt.JwtTokenHandler;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import javax.ejb.EJB;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.File;
import java.util.Arrays;

@ArquillianSuiteDeployment
public abstract class BuildIncidentTestDeployment {

    public static String USER_NAME = "user";

    @EJB
    private JwtTokenHandler tokenHandler;

    private String token;

    @Deployment(name = "incident", order = 2)
    public static Archive<?> createDeployment() {
        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "incident.war");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
        testWar.addAsLibraries(files);

        testWar.addPackages(true, "eu.europa.ec.fisheries.uvms.incident");
        testWar.addAsResource("persistence.xml", "META-INF/persistence.xml");

        testWar.addAsWebInfResource("mock-web.xml", "web.xml");

        return testWar;
    }

    @Deployment(name = "uvms", order = 1)
    public static Archive<?> createUVMSMock() {
        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "unionvms.war");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .importRuntimeAndTestDependencies()
                .resolve()
                .withTransitivity().asFile();
        testWar.addAsLibraries(files);

        testWar.addClass(SpatialModuleMock.class);
        testWar.addClass(UnionVMSRestMock.class);
        testWar.addClass(MovementMock.class);
        testWar.addClass(AssetMock.class);

        return testWar;
    }

    protected WebTarget getWebTarget() {
        Client client = ClientBuilder.newClient();
        client.register(JsonBConfigurator.class);
        return client.target("http://localhost:8080/incident/rest");
    }

    protected String getToken() {
        if (token == null) {
            token = tokenHandler.createToken(USER_NAME,
                    Arrays.asList(UnionVMSFeature.manageManualMovements.getFeatureId(),
                            UnionVMSFeature.viewMovements.getFeatureId(),
                            UnionVMSFeature.viewManualMovements.getFeatureId(),
                            UnionVMSFeature.manageAlarmsHoldingTable.getFeatureId(),
                            UnionVMSFeature.viewAlarmsHoldingTable.getFeatureId(),
                            UnionVMSFeature.viewAlarmsOpenTickets.getFeatureId()));
        }
        return token;
    }

}
