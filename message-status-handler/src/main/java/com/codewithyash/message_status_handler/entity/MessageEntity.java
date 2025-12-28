package com.codewithyash.message_status_handler.entity;

import com.codewithyash.message_status_handler.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@Table(name = "messages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id"})
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private String messageId; // UUID from producer
    private String sender;
    private String receiver;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    private Instant createdAt;
}
