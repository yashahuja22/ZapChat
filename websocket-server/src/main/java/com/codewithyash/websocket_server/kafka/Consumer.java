package com.codewithyash.websocket_server.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {
    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${app.kafka.group-id}")
    public void consume(String message) {
        System.out.println("message received from kafka: " + message);
    }
}
