# Real-Time Chat Backend (WebSocket + Kafka)

This project is a real-time chat backend built to work reliably in a distributed setup.
The focus is not just sending messages instantly, but also not losing messages, even when users disconnect, servers restart, or multiple instances are running.

It uses WebSockets for real-time delivery, Kafka for durability and routing, Redis for coordination, and PostgreSQL for storage.

## High-level idea
* Every message is first written to Kafka

* Delivery is best-effort + recoverable

* WebSocket servers are stateless across instances

* Redis is used only for routing hints, not as a source of truth

* Database is the final source of truth


## Message flow (step by step)

### Sending a message
1.	Client sends message via WebSocket
2.	Server immediately publishes the message to Kafka with status UNDELIVERED
3.	Server checks:
* Is receiver connected locally?
* Is receiver connected on another instance (via Redis)?
4.	If deliverable:
* Message is sent via WebSocket
* A DELIVERED event is published to Kafka
5.	A separate consumer updates the database

Important:
Kafka write happens before delivery logic → message is never lost.


### If receiver is offline
* Message remains UNDELIVERED in DB
* No retries, no guessing
* Delivery happens when the user reconnects

### When user reconnects
1.	User connects via WebSocket
2.	Server:
* Registers user in Redis
* Adds user to local in-memory map
3.	Undelivered messages are fetched asynchronously
4.	Messages are sent one by one
5.	Status is updated to DELIVERED

WebSocket handshake stays fast — replay happens in background.

## Kafka usage

### Topics

### CHAT-MESSAGES

* Stores message state changes (UNDELIVERED, DELIVERED)

### CHAT-ROUTING

* Used when sender and receiver are connected to different servers

### Kafka is used as:
* A durable buffer

* A coordination mechanism

* A replay-safe event stream

## Database design

### messages table

Key points:

* message_id is globally unique

* UPSERT is used (ON CONFLICT)

* Consumer can safely retry events

`message_id UNIQUE`

### This allows:

* No duplicate inserts

* Safe Kafka reprocessing

* Idempotent writes


## Redis usage

### Redis only stores:
`username -> server-id`

This is not critical state.

### If Redis goes down:

* Messages are still written to Kafka

* Messages are not lost

* Worst case: message is delivered on reconnect

This was a deliberate design choice.

## WebSocket design decisions
* In-memory map per server for active connections

* No cross-server socket sharing

* No blocking DB calls during handshake

* Message replay happens in a background thread pool

This keeps connection latency predictable.

## Async message replay

### Why async?

### A user may have:
* 10 messages → fine

* 1,000+ messages → handshake would block

So:

* Handshake stays fast

* Replay happens using a ThreadPoolTaskExecutor

* Messages are fetched in pages

This keeps the system responsive.

## Running the app

### Build

`mvn clean package`

### Run one instance

`java -jar target/websocket-server-0.0.1-SNAPSHOT.jar \
--server.port=8081 \
--app.server-id=ws-1`

### Run multiple instances

`java -jar websocket-server.jar --server.port=8081 --app.server-id=ws-1
java -jar websocket-server.jar --server.port=8082 --app.server-id=ws-2`

### Background run

`nohup java -jar websocket-server.jar \
--server.port=8081 \
--app.server-id=ws-1 &`

## Logging
* JSON logs

* File-based logging

* Rotated at 20MB

* File names include server-id and port

* Ready for ELK ingestion

## What this project is good at
* Real-time messaging
* Offline message handling
* Horizontal scaling
* Failure tolerance
* Clean separation of responsibilities

## What it does NOT try to solve (yet)
* Exactly-once delivery
* Message ordering across partitions
* Read receipts
* Authentication / authorization
* Push notifications

Those can be added later.

## Author

Yash Ahuja \
Backend Engineer \
Interested in distributed systems, Kafka, Redis, and real-time architectures.