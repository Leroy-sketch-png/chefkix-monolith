package com.chefkix.social.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalMessage {
    private String type; // "join", "offer", "answer", "ice-candidate", "leave"
    private String conversationId; // The unique ID of the conversation/call
    private String senderId; // Who is sending this
    private Object data; // and WebRTC payload (SDP string or JSON object for ICE candidates)
}
