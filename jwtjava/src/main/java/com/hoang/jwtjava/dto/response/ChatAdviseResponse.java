package com.hoang.jwtjava.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatAdviseResponse {
    private String reply;
    private String sessionId;
    private List<ChatProductResponse> products;
    private String disclaimer;
}
