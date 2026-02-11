package com.codewithyash.message_status_handler.kafka;

import com.codewithyash.message_status_handler.entity.MessageEntity;
import com.codewithyash.message_status_handler.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class Consumer {
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;

    @Value("${app.kafka.messages-event-topic}")
    private String topic;

    @KafkaListener(topics = "${app.kafka.messages-event-topic}", groupId = "${app.kafka.group-id}")
    public void consumeEvent(String msgJson, String key) {
        try {
            log.info("Received: {}", msgJson);

            MessageEntity event = objectMapper.readValue(msgJson, MessageEntity.class);

            messageRepository.upsertMessage(
                    event.getMessageId(),
                    event.getSender(),
                    event.getReceiver(),
                    event.getMessage(),
                    event.getStatus().name(),
                    event.getCreatedAt()
            );

            log.info(
                    "Message [{}] upserted with status {}",
                    event.getMessageId(),
                    event.getStatus()
            );

        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }
}
