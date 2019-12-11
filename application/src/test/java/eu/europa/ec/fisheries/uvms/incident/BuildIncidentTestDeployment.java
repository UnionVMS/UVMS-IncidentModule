package eu.europa.ec.fisheries.uvms.incident;

import java.io.File;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ArquillianSuiteDeployment
public abstract class BuildIncidentTestDeployment {

    final static Logger LOG = LoggerFactory.getLogger(BuildIncidentTestDeployment.class);

    @Deployment(name = "incident", order = 2)
    public static Archive<?> createDeployment() {
        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "incident.war");

        File[] files = Maven.resolver().loadPomFromFile("pom.xml")
                .importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();
        testWar.addAsLibraries(files);
        
        testWar.addPackages(true, "eu.europa.ec.fisheries.uvms.incident");

        testWar.addAsResource("persistence.xml", "META-INF/persistence.xml");

		return testWar;
	}
}
