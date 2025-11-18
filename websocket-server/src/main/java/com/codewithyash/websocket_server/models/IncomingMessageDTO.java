package com.codewithyash.websocket_server.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomingMessageDTO {
    private String to;
    private String content;
}
