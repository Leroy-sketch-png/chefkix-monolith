package com.chefkix.social.group.controller;

import java.util.List;

import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.dto.response.JoinGroupResponse;
import com.chefkix.social.group.service.GroupService;
import jakarta.validation.Valid;

import com.chefkix.shared.dto.ApiResponse;
import com.chefkix.social.chat.dto.request.ChatMessageRequest;
import com.chefkix.social.chat.dto.response.ChatMessageResponse;
import com.chefkix.social.chat.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    @Autowired
    MongoTemplate mongoTemplate;


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
        log.info("groupId: {}", groupId);
        log.info("currentUserId: {}", currentUserId);
        log.info("Current DB: {}", mongoTemplate.getDb().getName());

        JoinGroupResponse response = groupService.handleJoinRequest(groupId, currentUserId);

        log.info("Joining group: {}", groupId);

        return ApiResponse.created(response);
    }
}
