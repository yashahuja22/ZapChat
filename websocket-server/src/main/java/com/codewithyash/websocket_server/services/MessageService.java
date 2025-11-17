package com.codewithyash.websocket_server.services;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    public List<Messages> getAllUndeliveredMessages(String username) {
        return messageRepository.findByReceiverAndStatusOrderByCreatedAtAsc(username, MessageStatus.UNDELIVERED);
    }
}
