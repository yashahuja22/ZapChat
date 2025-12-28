package com.codewithyash.websocket_server.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingEventDTO {
    private String messageid;
    private String sender;
    private String receiver;
    private String content;
}
