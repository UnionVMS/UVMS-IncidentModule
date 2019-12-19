package eu.europa.ec.fisheries.uvms.incident.arquillian;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.incident.BuildIncidentTestDeployment;
import eu.europa.ec.fisheries.uvms.incident.helper.JMSHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TicketHelper;
import eu.europa.ec.fisheries.uvms.incident.helper.TopicListener;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.IncidentDto;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class IncidentConsumerTest extends BuildIncidentTestDeployment {

    @Inject
    private JMSHelper jmsHelper;

    @Inject
    private TicketHelper ticketHelper;

    private ObjectMapper objectMapper = getObjectMapper();

    @Before
    public void clearExchangeQueue() throws Exception {
        jmsHelper.clearQueue(jmsHelper.QUEUE_NAME);
    }

    @Test
    @OperateOnDeployment("incident")
    public void consumeIncidentQueue() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        TicketType ticket = ticketHelper.createTicket(ticketId, assetId, movId, mobTermId);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = objectMapper.writeValueAsString(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString);

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = objectMapper.readValue(text, IncidentDto.class);
            assertEquals(assetId, incident.getAssetId());
        }
    }
}
