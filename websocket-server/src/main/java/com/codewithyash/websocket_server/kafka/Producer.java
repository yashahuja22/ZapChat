package com.codewithyash.websocket_server.kafka;

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
    private String kafkaTopic;

    public void publishMessageToKafka(String message) {
        kafkaTemplate.send(kafkaTopic, message);
        log.info("Message published to kafka {}", message);
    }
}
