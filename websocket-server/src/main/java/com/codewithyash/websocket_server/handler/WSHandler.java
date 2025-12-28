package com.codewithyash.websocket_server.handler;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.EventKeys;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.kafka.Producer;
import com.codewithyash.websocket_server.models.IncomingMessageDTO;
import com.codewithyash.websocket_server.models.MessageResDTO;
import com.codewithyash.websocket_server.models.RoutingEventDTO;
import com.codewithyash.websocket_server.services.HandleClients;
import com.codewithyash.websocket_server.services.MessageService;
import com.codewithyash.websocket_server.services.RedisService;
import com.codewithyash.websocket_server.services.UndeliveredMessageReplayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSHandler extends TextWebSocketHandler {

    private final HandleClients handleClients;

    private final Producer producer;

    private final MessageService messageService;

    @Value("${app.server-id}")
    private String serverId;

    private final RedisService redisService;

    private final ObjectMapper objectMapper;

    private final UndeliveredMessageReplayService undeliveredMessageReplayService;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        // step 1: set into redis. -- done
        // step 2: get data from DB for messages which are undelivered -- done
        // step 3: send them to client and produce event to kafka marked as delivered. -- done.
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


        undeliveredMessageReplayService.replay(username);

        log.info("replay started...");
//        List<Messages> allUndeliveredMessages = messageService.getAllUndeliveredMessages(username);
//
//        if(!allUndeliveredMessages.isEmpty()) {
//            for (Messages messages : allUndeliveredMessages) {
//                // TODO: handling of failures messages
//                Thread.sleep(1000);
//                handleClients.sendDataToClient(username, new MessageResDTO(messages.getSender(), messages.getMessage(), messages.getCreatedAt().toString()));
//
//                producer.publishMessagesEventToKafka(messageService.getMessageEventFormatInString(
//                        MessageStatus.DELIVERED,messages.getMessageId(), messages.getSender(), messages.getReceiver(), messages.getMessage()), EventKeys.UPDATE_MESSAGE_STATUS);
//            }
//        } else {
//            log.info("no undelivered messages found for username {}", username);
//        }
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

        // Publish message to kafka
        Messages messageDetails = messageService.getMessageEventFormat(
                MessageStatus.UNDELIVERED,null, sender, receiver, content
        );

        producer.publishMessagesEventToKafka(objectMapper.writeValueAsString(messageDetails),
                EventKeys.NEW_MESSAGE_STATUS);


        if(handleClients.isClientConnected(messageDetails.getReceiver())) {
            // Receiver is connected locally
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(formatter);

            log.info("Receiver {} is connected locally. Sending WS message.", receiver);

            MessageResDTO msgToDeliver = new MessageResDTO(
                    sender,
                    content,
                    istTime
            );

            // send directly to WebSocket client
            handleClients.sendDataToClient(receiver, msgToDeliver);

            // Publish Deliver event to kafka
            producer.publishMessagesEventToKafka(messageService.getMessageEventFormatInString(
                    MessageStatus.DELIVERED,
                    messageDetails.getMessageId(),
                    messageDetails.getSender(),
                    messageDetails.getReceiver(),
                    content), EventKeys.NEW_MESSAGE_STATUS);
        }

        // Checking receiver is connected with another instance or not
        else if(redisService.userExistInRedis(receiver)) {
            // Receiver is connected with another instance
            log.info("{} receiver is connected with another server routing event", receiver);
            producer.publishMessageRoutingEventToKafka(objectMapper.writeValueAsString(RoutingEventDTO
                    .builder()
                            .messageid(messageDetails.getMessageId())
                            .sender(sender)
                            .receiver(receiver)
                            .content(content)
                    .build()));
        }

//        // Receiver is offline
//        else {
//            // Receiver is offline
//            log.info("{} is not connected anywhere, assuming user is offline", receiver);
//
//            producer.publishMessagesEventToKafka(messageService.getMessageEventFormat(
//                    MessageStatus.UNDELIVERED,null, sender, receiver, content
//            ),EventKeys.NEW_MESSAGE_STATUS);
//        }
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
