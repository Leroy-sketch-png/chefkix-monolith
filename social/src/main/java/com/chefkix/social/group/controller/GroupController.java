package com.chefkix.social.group.controller;

import java.util.List;

import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.service.GroupService;
import jakarta.validation.Valid;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.service.ChatMessageService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupController {
    GroupService groupService;

    @PostMapping()
    ApiResponse<GroupResponse> create(@RequestBody @Valid GroupCreationRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.created(groupService.createGroup(request, userId));
    }
}
