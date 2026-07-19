package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.ChatAdviseRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.ChatAdviseResponse;
import com.hoang.jwtjava.service.ProductChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat")
public class ChatController {

    private final ProductChatService productChatService;

    @PostMapping("/advise")
    @Operation(
            summary = "Product shopping advisor",
            description = """
                    Chatbot tư vấn sản phẩm công khai (không cần JWT).
                    Backend gọi Gemini và chỉ gợi ý theo catalog hiện có.
                    Gửi `sessionId` (UUID) để nhớ tối đa 3 lượt hỏi–đáp gần nhất (Redis TTL 30 phút).
                    Response `products` luôn lấy từ DB, không tin số liệu do model bịa.
                    """)
    public ResponseEntity<ApiResponse<ChatAdviseResponse>> advise(
            @RequestBody @Valid ChatAdviseRequest request) {
        return ResponseEntity.ok(ApiResponse.<ChatAdviseResponse>builder()
                .message("Chat advice generated")
                .result(productChatService.advise(request))
                .build());
    }
}
