package com.chefkix.social.group.controller;

import java.util.List;

import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.dto.response.JoinGroupResponse;
import com.chefkix.social.group.dto.response.PendingRequestResponse;
import com.chefkix.social.group.service.GroupService;
import jakarta.validation.Valid;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Slf4j
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

    @PostMapping("/{groupId}/join")
    ApiResponse<JoinGroupResponse> joinGroup(
            @PathVariable("groupId") String groupId
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        JoinGroupResponse response = groupService.handleJoinRequest(groupId, currentUserId);
        return ApiResponse.created(response);
    }

    @DeleteMapping("/{groupId}/leave")
    public ApiResponse<String> leaveGroup(
            @PathVariable("groupId") String groupId
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        groupService.handleLeaveOrCancel(groupId, currentUserId);

        return ApiResponse.success("Successfully left group"); // 204 No Content
    }

    @GetMapping("/{groupId}/requests")
    public ApiResponse<Page<PendingRequestResponse>> getPendingRequests(
            @PathVariable("groupId") String groupId,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Page<PendingRequestResponse> requests = groupService.getPendingRequests(groupId, currentUserId, pageable);
        return ApiResponse.success(requests);
    }
}
