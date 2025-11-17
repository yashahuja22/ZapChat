package com.codewithyash.websocket_server.handler;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.kafka.Producer;
import com.codewithyash.websocket_server.services.HandleClients;
import com.codewithyash.websocket_server.services.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSHandler extends TextWebSocketHandler {

    private final HandleClients handleClients;

    @Autowired
    private Producer producer;

    private final MessageService messageService;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        // step 1: set into redis.
        // step 2: get data from DB for messages which are undelivered
        // step 3: send them to client and produce event to kafka marked as delivered.
        String username = webSocketSession.getHandshakeHeaders().getFirst("username");
        if (username == null) {
            webSocketSession.sendMessage(new TextMessage("username not found in headers"));
            webSocketSession.close();
            return;
        }
        handleClients.addClient(username, webSocketSession);

        log.info("client with username {} connected successfully, getting all the undelivered messages", username);

        List<Messages> allUndeliveredMessages = messageService.getAllUndeliveredMessages(username);

        if(!allUndeliveredMessages.isEmpty()) {
            for (Messages messages : allUndeliveredMessages) {
                handleClients.sendDataToClient(username, messages.getMessage());
            }
//            System.out.println(allUndeliveredMessages);
        } else {
            log.info("no undelivered messages found for username {}", username);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
        // step 1: check that client is connected with us or not.
        // step 2: if yes -> send data directly and produce it to kafka as delivered.
        // step 3: if no -> check in redis, if found send data to kafka, else produce to kafka as undelivered

        producer.publishMessageToKafka(message.getPayload());

        webSocketSession.sendMessage(new TextMessage(message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws IOException {
        // At disconnect we need to remove entry for this username from redis
        String username = webSocketSession.getHandshakeHeaders().getFirst("username");

        if (username == null) {
            webSocketSession.sendMessage(new TextMessage("username not found in headers"));
            webSocketSession.close();
            return;
        }

        handleClients.removeClient(username);
    }
}
