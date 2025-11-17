package com.codewithyash.websocket_server.config;

import com.codewithyash.websocket_server.entities.Messages;
import com.codewithyash.websocket_server.enums.MessageStatus;
import com.codewithyash.websocket_server.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final MessageRepository messageRepository;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("🔄 Loading dummy messages into DB...");

        // Insert only if DB is empty
        if (messageRepository.count() == 0) {

            Messages m1 = new Messages(
                    null,
                    UUID.randomUUID().toString(),
                    "alice",
                    "yash_ahuja",
                    "Hi Bob, how are you?",
                    MessageStatus.UNDELIVERED,
                    Instant.now()
            );

            Messages m2 = new Messages(
                    null,
                    UUID.randomUUID().toString(),
                    "bob",
                    "alice",
                    "Hey Alice! I'm good.",
                    MessageStatus.DELIVERED,
                    Instant.now()
            );

            Messages m3 = new Messages(
                    null,
                    UUID.randomUUID().toString(),
                    "john",
                    "yash_ahuja",
                    "Will you join the meeting?",
                    MessageStatus.UNDELIVERED,
                    Instant.now()
            );

            messageRepository.save(m1);
            messageRepository.save(m2);
            messageRepository.save(m3);

            System.out.println("✅ Dummy messages inserted.");
        } else {
            System.out.println("ℹ️ Messages table not empty, skipping dummy data load.");
        }
    }
}
