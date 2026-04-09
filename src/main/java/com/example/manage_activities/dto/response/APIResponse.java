package com.example.manage_activities.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {
    int code;
    String message;
    T result;

    public static <T> APIResponse<T> response(T result) {
        return APIResponse.<T>builder()
                .code(200)
                .message("Success")
                .result(result)
                .build();
    }


    public static class APIResponseBuilder<T> {
        private int code;
        private String message;
        private T result;

        public APIResponseBuilder<T> code(int code) {
            this.code = code;
            return this;
        }

        public APIResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public APIResponseBuilder<T> result(T result) {
            this.result = result;
            return this;
        }

        public APIResponse<T> build() {
            APIResponse<T> response = new APIResponse<>();
            response.code = this.code != 0 ? this.code : 200;
            response.message = this.message != null ? this.message : "Success";
            response.result = this.result;
            return response;
        }
    }
}