package com.chefkix.identity.mapper;

import com.chefkix.identity.dto.response.ProfileResponse;
import com.chefkix.identity.dto.response.UserProfileResponse;
import com.chefkix.identity.entity.SignupRequest;
import com.chefkix.identity.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = StatisticsMapper.class)
public interface ProfileMapper {
  @Mapping(source = "createdAt", target = "createdAt")
  @Mapping(source = "updatedAt", target = "updatedAt")
  @Mapping(source = "id", target = "profileId")
  ProfileResponse toProfileResponse(UserProfile profile);

  UserProfile toProfile(SignupRequest request);

  UserProfileResponse toUserProfileResponse(UserProfile profileResponse);
}
