package com.codewithyash.websocket_server.kafka;

import com.codewithyash.websocket_server.enums.EventKeys;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.models.MessageResDTO;
import com.codewithyash.websocket_server.models.RoutingEventDTO;
import com.codewithyash.websocket_server.services.HandleClients;
import com.codewithyash.websocket_server.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class Consumer {
    private final HandleClients handleClients;
    private final ObjectMapper objectMapper;
    private final Producer producer;
    private final MessageService messageService;

    @KafkaListener(topics = "${app.kafka.routing-topic}", groupId = "${app.kafka.group-id}")
    public void consume(String message) {
        try {
            log.info("routing message received from kafka: {}", message);

            RoutingEventDTO routingEventDTO = objectMapper.readValue(message, RoutingEventDTO.class);

            if(handleClients.isClientConnected(routingEventDTO.getReceiver())) {
                log.info("Receiver '{}' is connected on this server. Delivering message.",
                        routingEventDTO.getReceiver());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(formatter);

                handleClients.sendDataToClient(routingEventDTO.getReceiver(), new MessageResDTO(
                        routingEventDTO.getSender(), routingEventDTO.getContent(), istTime
                ));

                log.info("Message delivered to WebSocket client: receiver={}",
                        routingEventDTO.getReceiver());

                producer.publishMessagesEventToKafka(messageService.getMessageEventFormatInString(
                        MessageStatus.DELIVERED,
                                routingEventDTO.getMessageid(),
                        routingEventDTO.getSender(),
                        routingEventDTO.getReceiver(),
                        routingEventDTO.getContent()),
                        EventKeys.NEW_MESSAGE_STATUS);

                log.info("DELIVERED event published to Kafka for receiver={}",
                        routingEventDTO.getReceiver());
            } else {
                log.info("Receiver '{}' is NOT connected on this server. Skipping delivery.",
                        routingEventDTO.getReceiver());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
