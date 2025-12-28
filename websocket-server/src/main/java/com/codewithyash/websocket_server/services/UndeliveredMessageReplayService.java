package com.codewithyash.websocket_server.services;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.EventKeys;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.kafka.Producer;
import com.codewithyash.websocket_server.models.MessageResDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UndeliveredMessageReplayService {
    private final MessageService messageService;
    private final HandleClients handleClients;
    private final Producer producer;


    @Async("undeliveredExecutor")
    public void replay(String username) throws JsonProcessingException {
        log.info("Starting undelivered replay for {}", username);

        int page = 0;
        int size = 50;

        Page<Messages> batch;

        do {
            batch = messageService.getUndeliveredMessages(username, page, size);

            for (Messages msg : batch.getContent()) {

                // user might disconnect mid-replay
                if (!handleClients.isClientConnected(username)) {
                    log.warn("User {} disconnected during replay, stopping", username);
                    return;
                }

                handleClients.sendDataToClient(
                        username,
                        new MessageResDTO(
                                msg.getSender(),
                                msg.getMessage(),
                                msg.getCreatedAt().toString()
                        )
                );

                producer.publishMessagesEventToKafka(
                        messageService.getMessageEventFormatInString(
                                MessageStatus.DELIVERED,
                                msg.getMessageId(),
                                msg.getSender(),
                                msg.getReceiver(),
                                msg.getMessage()
                        ),
                        EventKeys.UPDATE_MESSAGE_STATUS
                );
            }

            page++;

        } while (!batch.isEmpty());

        log.info("Completed undelivered replay for {}", username);
    }
}
