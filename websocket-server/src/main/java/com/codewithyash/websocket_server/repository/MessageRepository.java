package com.codewithyash.websocket_server.repository;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Messages, Long> {
    List<Messages> findByReceiverAndStatusOrderByCreatedAtAsc(String receiver, MessageStatus status);
}
