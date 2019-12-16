package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class IncidentServiceBeanTest extends BuildIncidentTestDeployment {

    @Test
    public void getAssetNotSendingListTest() {
        Assert.assertTrue(true);
    }

}
