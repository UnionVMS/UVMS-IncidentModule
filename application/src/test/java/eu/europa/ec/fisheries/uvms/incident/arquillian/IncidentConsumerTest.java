package eu.europa.ec.fisheries.uvms.incident.arquillian;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.incident.TransactionalTests;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.IncidentDto;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class IncidentConsumerTest extends TransactionalTests {

    @Inject
    private JMSHelper jmsHelper;

    private static final String QUEUE_NAME = "IncidentEvent";
    public static final String EVENT_STREAM = "jms.topic.EventStream";

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules();
    }

    @Before
    public void clearExchangeQueue() throws Exception {
        jmsHelper.clearQueue(QUEUE_NAME);
    }

    @Test
    @OperateOnDeployment("incident")
    public void consumeIncidentQueue() throws Exception {
        String ticketId = UUID.randomUUID().toString();
        String assetId = UUID.randomUUID().toString();
        String movId = UUID.randomUUID().toString();
        String mobTermId = UUID.randomUUID().toString();
        TicketType ticket = createTicket(ticketId, assetId, movId, mobTermId);

        try (TopicListener listener = new TopicListener(EVENT_STREAM, "")) {
            String asString = objectMapper.writeValueAsString(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString);

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = objectMapper.readValue(text, IncidentDto.class);
            assertEquals(assetId, incident.getAssetId().toString());
        }
    }

    private TicketType createTicket(String ticketId, String assetId, String movId, String mobTermId) {
        TicketType ticket = new TicketType();
        ticket.setGuid(ticketId);
        ticket.setAssetGuid(assetId);
        ticket.setMovementGuid(movId);
        ticket.setMobileTerminalGuid(mobTermId);
        ticket.setRuleName("Asset not sending");
        ticket.setRuleGuid("Asset not sending");
        ticket.setUpdatedBy("UVMS");
        ticket.setStatus(TicketStatusType.POLL_PENDING);
        ticket.setTicketCount(1L);
        String date = String.valueOf(Instant.now().getEpochSecond());
        ticket.setOpenDate(date);
        ticket.setUpdated(date);
        return ticket;
    }
}
