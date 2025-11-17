package com.codewithyash.websocket_server.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Producer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic}")
    private String kafkaTopic;

    public void publishMessageToKafka(String message) {
        kafkaTemplate.send(kafkaTopic,message);
        System.out.println("Message published to kafka "+ message);
    }
}
