package com.example.manage_activities.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequest {

    @NotBlank(message = "USERNAME_INVALID")
    String username;

    @NotBlank
    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    @Email(message = "BAD_REQUEST")
    String email;

    @Min(value = 1, message = "BAD_REQUEST")
    @Max(value = 4, message = "BAD_REQUEST")
    Integer roleId;

    String status;

    @Valid
    UserIdentityRequest identity;
}
