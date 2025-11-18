package com.codewithyash.websocket_server.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResDTO {
    private String sender;
    private String content;
    private String time;
}
