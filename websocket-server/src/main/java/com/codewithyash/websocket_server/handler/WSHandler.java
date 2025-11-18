package com.codewithyash.websocket_server.handler;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.kafka.Producer;
import com.codewithyash.websocket_server.models.IncomingMessageDTO;
import com.codewithyash.websocket_server.models.MessageResDTO;
import com.codewithyash.websocket_server.services.HandleClients;
import com.codewithyash.websocket_server.services.MessageService;
import com.codewithyash.websocket_server.services.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSHandler extends TextWebSocketHandler {

    private final HandleClients handleClients;

    @Autowired
    private Producer producer;

    private final MessageService messageService;

    @Value("${app.server-id}")
    private String serverId;

    private final RedisService redisService;

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        // step 1: set into redis. -- done
        // step 2: get data from DB for messages which are undelivered -- done
        // step 3: send them to client and produce event to kafka marked as delivered.
        String username = webSocketSession.getHandshakeHeaders().getFirst("username");
        if (username == null) {
            log.error("username not found in headers");
            webSocketSession.sendMessage(new TextMessage("username not found in headers"));
            webSocketSession.close();
            return;
        }

        // Setting user into redis
        redisService.setUserIntoRedis(username, serverId);

        handleClients.addClient(username, webSocketSession);

        webSocketSession.getAttributes().put("username", username);

        log.info("client with username {} connected successfully, getting all the undelivered messages", username);

        List<Messages> allUndeliveredMessages = messageService.getAllUndeliveredMessages(username);

        if(!allUndeliveredMessages.isEmpty()) {
            for (Messages messages : allUndeliveredMessages) {
                // TODO: handling of failures messages
                handleClients.sendDataToClient(username, new MessageResDTO(messages.getSender(), messages.getMessage(), messages.getCreatedAt().toString()));
            }
        } else {
            log.info("no undelivered messages found for username {}", username);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
        String sender = (String) webSocketSession.getAttributes().get("username");
        if (sender == null) {
            log.error("Sender username missing in session.");
            return;
        }

        log.info("Message received from {} => {}", sender, message.getPayload());


        IncomingMessageDTO incomingMessage =
                objectMapper.readValue(message.getPayload(), IncomingMessageDTO.class);

        String receiver = incomingMessage.getTo();
        String content  = incomingMessage.getContent();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(formatter);

        // step 1: check that client is connected with us or not.
        // step 2: if yes -> send data directly and produce it to kafka as delivered.
        if(handleClients.isClientConnected(receiver)) {
            log.info("Receiver {} is connected locally. Sending WS message.", receiver);

            MessageResDTO msgToDeliver = new MessageResDTO(
                    sender,
                    content,
                    istTime
            );

            // send directly to WebSocket client
            handleClients.sendDataToClient(receiver, msgToDeliver);

            // Publish Deliver event to kafka
            String msgJson = objectMapper.writeValueAsString(Messages.builder()
                    .messageId(UUID.randomUUID().toString())
                    .sender(sender)
                    .receiver(receiver)
                    .message(content)
                    .status(MessageStatus.DELIVERED)
                    .createdAt(Instant.now())
                    .build()
            );

            producer.publishMessageToKafka(msgJson);

            return;
        }

        log.info("{} is not connected with us, assuming user is offline", receiver);

        String msgJson = objectMapper.writeValueAsString(Messages.builder()
                .messageId(UUID.randomUUID().toString())
                .sender(sender)
                .receiver(receiver)
                .message(content)
                .status(MessageStatus.UNDELIVERED)
                .createdAt(Instant.now())
                .build()
        );

        producer.publishMessageToKafka(msgJson);
        // step 3: if no -> check in redis, if found send data to kafka, else produce to kafka as undelivered

    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) {
        // At disconnect we need to remove entry for this username from redis
        String username = (String) webSocketSession.getAttributes().get("username");

        if (username != null) {
            handleClients.removeClient(username);

            // remove username from redis
            redisService.removeUser(username);
        }
    }
}
