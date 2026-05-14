package com.example.manage_activities.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST), 
    BAD_REQUEST(1009, "Bad request", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1010, "Email existed", HttpStatus.BAD_REQUEST),
    NO_REGISTRATIONS(1011, "No registrations found for user", HttpStatus.NOT_FOUND),
    EXIST_REGISTRATIONS(1012, "User is already registered for this activity", HttpStatus.BAD_REQUEST),
    EXIST_PROFILE(1013, "Profile already exists for this user", HttpStatus.BAD_REQUEST),
    DONT_EXIST_PROFILE(1014, "Profile not found for this user", HttpStatus.NOT_FOUND),
    ACTIVITY_NOT_FOUND(1018, "Activity not found", HttpStatus.NOT_FOUND),
    ACTIVITY_ALREADY_APPROVED(1019, "Activity has already been approved", HttpStatus.BAD_REQUEST),
    ACTIVITY_ALREADY_REJECTED(1020, "Activity has already been rejected", HttpStatus.BAD_REQUEST),
    REGISTRATION_NOT_FOUND(1021, "Registration not found", HttpStatus.NOT_FOUND),
    REGISTRATION_ALREADY_APPROVED(1015, "Registration has already been approved", HttpStatus.BAD_REQUEST),
    REGISTRATION_CANCELLED(1016, "Registration was cancelled", HttpStatus.BAD_REQUEST),
    REGISTRATION_ALREADY_REJECTED(1017, "Registration has already been rejected", HttpStatus.BAD_REQUEST),
    ACTIVITY_NOT_AVAILABLE_FOR_REGISTRATION(1022, "Activity is not available for registration", HttpStatus.BAD_REQUEST),
    REGISTRATION_DEADLINE_EXPIRED(1023, "Registration deadline has expired", HttpStatus.BAD_REQUEST),
    ACTIVITY_FULL(1024, "Activity has reached maximum participants", HttpStatus.BAD_REQUEST),
    REGISTRATION_CANNOT_CANCEL(1025, "Registration cannot be cancelled", HttpStatus.BAD_REQUEST),
    NOTIFICATION_NOT_FOUND(1026, "Notification not found", HttpStatus.NOT_FOUND);


    private int code;
    private String message;
    private HttpStatusCode httpStatus;

    ErrorCode(int code, String message, HttpStatusCode httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

}
