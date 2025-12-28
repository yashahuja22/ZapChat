package com.codewithyash.websocket_server.services;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public List<Messages> getAllUndeliveredMessages(String username) {
        return messageRepository.findByReceiverAndStatusOrderByCreatedAtAsc(username, MessageStatus.UNDELIVERED);
    }

    public String getMessageEventFormatInString(MessageStatus status, String messageId, String sender, String receiver, String content) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Messages.builder()
                .messageId(messageId == null ? UUID.randomUUID().toString(): messageId)
                .sender(sender)
                .receiver(receiver)
                .message(content)
                .status(status)
                .createdAt(Instant.now())
                .build()
        );
    }

    public Messages getMessageEventFormat(MessageStatus status, String messageId, String sender, String receiver, String content) throws JsonProcessingException {
        return Messages.builder()
                .messageId(messageId == null ? UUID.randomUUID().toString(): messageId)
                .sender(sender)
                .receiver(receiver)
                .message(content)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    public Page<Messages> getUndeliveredMessages(String receiver, int page, int size) {
        return messageRepository.findByReceiverAndStatusOrderByCreatedAtAsc(
                receiver,
                MessageStatus.UNDELIVERED,
                PageRequest.of(page, size)
        );
    }
}
