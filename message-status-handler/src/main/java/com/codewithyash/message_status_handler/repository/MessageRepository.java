package com.codewithyash.message_status_handler.repository;

import com.codewithyash.message_status_handler.entity.MessageEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO messages (
            message_id,
            sender,
            receiver,
            message,
            status,
            created_at
        )
        VALUES (
            :messageId,
            :sender,
            :receiver,
            :message,
            :status,
            :createdAt
        )
        ON CONFLICT (message_id)
        DO UPDATE SET
            status = EXCLUDED.status
        """, nativeQuery = true)
    void upsertMessage(
            @Param("messageId") String messageId,
            @Param("sender") String sender,
            @Param("receiver") String receiver,
            @Param("message") String message,
            @Param("status") String status,
            @Param("createdAt") Instant createdAt
    );
}
