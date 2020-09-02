package eu.europa.ec.fisheries.uvms.incident.arquillian;

import eu.europa.ec.fisheries.uvms.commons.date.JsonBConfigurator;
import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TopicListener;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentDto;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class IncidentConsumerTest extends BuildIncidentTestDeployment {

    @Inject
    private JMSHelper jmsHelper;


    private Jsonb jsonb;

    {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Before
    public void clearExchangeQueue() throws Exception {
        jmsHelper.clearQueue(jmsHelper.QUEUE_NAME);
    }

    @Test
    @OperateOnDeployment("incident")
    public void consumeIncidentQueue() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket( assetId, movId, mobTermId);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = jsonb.toJson(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = jsonb.fromJson(text, IncidentDto.class);
            assertEquals(assetId, incident.getAssetId());
        }
    }

    @Test
    @OperateOnDeployment("incident")
    public void consumeIncidentQueueWoTicket() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movId, mobTermId);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = jsonb.toJson(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = jsonb.fromJson(text, IncidentDto.class);
            assertEquals(assetId, incident.getAssetId());
        }
    }
}
