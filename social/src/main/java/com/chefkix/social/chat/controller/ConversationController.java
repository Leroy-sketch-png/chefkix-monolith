package com.chefkix.social.chat.controller;

import java.util.List;

import jakarta.validation.Valid;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.chat.dto.request.ConversationRequest;
import com.chefkix.social.chat.dto.response.ConversationResponse;
import com.chefkix.social.chat.dto.response.ShareContactResponse;
import com.chefkix.social.chat.service.ConversationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/conversations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
    ConversationService conversationService;

    @PostMapping("/create")
    ApiResponse<ConversationResponse> createConversation(@RequestBody @Valid ConversationRequest request) {
        return ApiResponse.created(conversationService.create(request));
    }

    @GetMapping("/my-conversations")
    ApiResponse<List<ConversationResponse>> myConversations() {
        return ApiResponse.success(conversationService.myConversations());
    }

    @GetMapping("/share-suggestions")
    public ApiResponse<List<ShareContactResponse>> getShareSuggestions(@PageableDefault(size = 5) Pageable pageable) {
        var result = conversationService.getShareSuggestions(pageable);
        return ApiResponse.success(result);
    }
}
