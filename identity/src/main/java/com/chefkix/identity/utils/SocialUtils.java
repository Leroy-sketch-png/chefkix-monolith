package com.chefkix.identity.utils;

import com.chefkix.identity.entity.Friendship;
import com.chefkix.identity.entity.UserProfile;
import com.chefkix.identity.enums.RelationshipStatus;
import com.chefkix.identity.repository.FriendRequestRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialUtils {
  final FriendRequestRepository friendRequestRepository;

  public RelationshipStatus determineRelationshipStatus(
      String currentUserId, UserProfile targetProfile) {
    String targetUserId = targetProfile.getUserId();

    // 1. Kiểm tra có phải chính mình không
    if (currentUserId.equals(targetUserId)) {
      return RelationshipStatus.SELF;
    }

    // 2. Kiểm tra xem đã là BẠN BÈ chưa
    // (Cách kiểm tra này giả định bạn đã có object UserProfile của CHÍNH MÌNH,
    // nếu không, bạn cần findOne profile của currentUserId trước)

    // Cách 1: Nếu bạn đã lưu List<Friendship> (như ta bàn)
    List<Friendship> friends =
        (targetProfile.getFriends() != null) ? targetProfile.getFriends() : Collections.emptyList();

    boolean isAlreadyFriends =
        friends.stream().anyMatch(f -> f.getFriendId().equals(currentUserId));
    if (isAlreadyFriends) {
      return RelationshipStatus.FRIENDS;
    }

    // 3. Nếu không phải là bạn, kiểm tra LỜI MỜI ĐÃ GỬI (REQUEST_SENT)
    // (Mình gửi cho họ)
    if (friendRequestRepository.existsBySenderIdAndReceiverId(currentUserId, targetUserId)) {
      return RelationshipStatus.REQUEST_SENT;
    }

    // 4. Nếu không, kiểm tra LỜI MỜI ĐÃ NHẬN (REQUEST_RECEIVED)
    // (Họ gửi cho mình)
    if (friendRequestRepository.existsBySenderIdAndReceiverId(targetUserId, currentUserId)) {
      return RelationshipStatus.REQUEST_RECEIVED;
    }

    // 5. Nếu không có gì, là NGƯỜI LẠ
    return RelationshipStatus.NOT_FRIENDS;
  }
}
