package com.chefkix.social.group.controller;

import com.chefkix.social.group.dto.query.GroupExploreQuery;
import com.chefkix.social.group.dto.request.GroupUpdateRequest;
import com.chefkix.social.group.dto.request.ProcessJoinRequest;
import com.chefkix.social.group.dto.request.GroupCreationRequest;
import com.chefkix.social.group.dto.request.TransferOwnershipRequest;
import com.chefkix.social.group.dto.response.GroupMemberResponse;
import com.chefkix.social.group.dto.response.GroupResponse;
import com.chefkix.social.group.dto.response.JoinGroupResponse;
import com.chefkix.social.group.dto.response.PendingRequestResponse;
import com.chefkix.social.group.service.GroupService;
import jakarta.validation.Valid;

import com.chefkix.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

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

    @PatchMapping("/{groupId}/requests/{userId}")
    public ApiResponse<String> processPendingRequest(
            @PathVariable("groupId") String groupId,
            @PathVariable("userId") String targetUserId,
            @RequestBody @Valid ProcessJoinRequest request
    ) {
        groupService.processJoinRequest(groupId, targetUserId, request.getAction());
        return ApiResponse.success("Request processed successfully");
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<String> kickMember(
            @PathVariable(value = "groupId") String groupId,
            @PathVariable(value = "userId") String targetUserId
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        groupService.kickMember(groupId, targetUserId, currentUserId);

        return ApiResponse.success("User has been removed from the group");
    }

    @PutMapping("/{groupId}/transfer")
    public ApiResponse<String> transferOwnership(
            @PathVariable("groupId") String groupId,
            @RequestBody @Valid TransferOwnershipRequest request
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Pass the password down to the service
        groupService.transferOwnership(groupId, request.getTargetUserId(), currentUserId, request.getPassword());

        return ApiResponse.success("Ownership has been successfully transferred");
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponse> getGroupDetails(
            @PathVariable("groupId") String groupId
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        GroupResponse response = groupService.getGroupDetails(groupId, currentUserId);

        return ApiResponse.success(response);
    }

    @GetMapping("/explore")
    public ApiResponse<Page<GroupResponse>> exploreGroups(
            // @ModelAttribute automatically maps URL parameters to your DTO fields!
            @ModelAttribute GroupExploreQuery query,

            // We only need page and size here, because our DTO handles the custom 'sortBy'
            @PageableDefault(size = 10) Pageable pageable
    ) {
        // 1. Get the current logged-in user
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Inject the user ID into the query object so the Service/Repository can use it
        query.setCurrentUserId(currentUserId);

        // 3. Execute the search
        Page<GroupResponse> responses = groupService.exploreGroups(query, pageable);

        // 4. Return the paginated results
        return ApiResponse.success(responses);
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<Page<GroupMemberResponse>> getGroupMembers(
            @PathVariable("groupId") String groupId,
            // Default: Show 20 members per page, newest first!
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Page<GroupMemberResponse> responses = groupService.getGroupMembers(groupId, currentUserId, pageable);

        return ApiResponse.success(responses);
    }

    @GetMapping("/me")
    public ApiResponse<Slice<GroupResponse>> getMyGroups(
            @RequestParam(value = "status", required = false) String status,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        // Pass the optional status down to the service
        Slice<GroupResponse> responses = groupService.getMyGroups(currentUserId, status, pageable);

        return ApiResponse.success(responses);
    }

    @PatchMapping("/{groupId}")
    public ApiResponse<GroupResponse> updateGroup(
            @PathVariable("groupId") String groupId,
            @RequestBody GroupUpdateRequest request
    ) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        GroupResponse response = groupService.updateGroup(groupId, request, currentUserId);

        return ApiResponse.success(response);
    }
}
