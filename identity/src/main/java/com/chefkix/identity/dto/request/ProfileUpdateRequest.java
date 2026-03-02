package com.chefkix.identity.dto.request;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO để xử lý yêu cầu cập nhật thông tin cá nhân của user. Các trường đều là optional, user gửi
 * trường nào thì cập nhật trường đó.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateRequest {

  // Personal info
  String firstName;
  String lastName;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate dob;

  String displayName;
  String phoneNumber;
  String avatarUrl;
  String coverImageUrl;
  String bio;
  String location;

  // Preferences & settings
  List<String> preferences;
}
