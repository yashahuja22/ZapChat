package com.codewithyash.websocket_server.kafka;

import com.codewithyash.websocket_server.enums.EventKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Producer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.messages-event-topic}")
    private String messagesEvent;

    @Value("${app.kafka.routing-topic}")
    private String messageRoutingEvent;

    public void publishMessageRoutingEventToKafka(String message) {
        kafkaTemplate.send(messageRoutingEvent, message);
        log.info("Message published to kafka topic {}, data: {}", messageRoutingEvent, message);
    }

    public void publishMessagesEventToKafka(String message, EventKeys key) {
        kafkaTemplate.send(messagesEvent, key.toString(), message);
        log.info("Message published to kafka topic {} ,data: {}", messagesEvent, message);
    }
}
