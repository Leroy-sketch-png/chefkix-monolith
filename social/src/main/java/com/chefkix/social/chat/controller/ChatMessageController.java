package com.chefkix.social.chat.controller;

import java.util.List;

import jakarta.validation.Valid;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.service.ChatMessageService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/messages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageController {
    ChatMessageService chatMessageService;

    @PostMapping("/create")
    ApiResponse<ChatMessageResponse> create(@RequestBody @Valid ChatMessageRequest request) {
        return ApiResponse.created(chatMessageService.create(request));
    }

    /**
     * Get all messages (non-paginated, for backwards compatibility).
     */
    @GetMapping
    ApiResponse<List<ChatMessageResponse>> getMessages(@RequestParam("conversationId") String conversationId) {
        return ApiResponse.success(chatMessageService.getMessages(conversationId));
    }

    /**
     * Get paginated messages for a conversation.
     * @param conversationId the conversation ID
     * @param page page number (0-indexed, defaults to 0)
     * @param size page size (defaults to 50)
     */
    @GetMapping("/paginated")
    ApiResponse<Page<ChatMessageResponse>> getMessagesPaginated(
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return ApiResponse.success(chatMessageService.getMessagesPaginated(conversationId, page, size));
    }
}
