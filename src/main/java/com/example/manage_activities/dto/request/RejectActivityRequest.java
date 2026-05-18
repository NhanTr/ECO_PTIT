package com.example.manage_activities.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
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
public class RejectActivityRequest {

    @NotBlank(message = "Reason must not be blank")
    @JsonAlias("reason")
    String rejectReason;

    /** @deprecated use {@link #getRejectReason()} */
    @Deprecated
    public String getReason() {
        return rejectReason;
    }
}
