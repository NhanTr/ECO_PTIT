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
    NOTIFICATION_NOT_FOUND(1026, "Notification not found", HttpStatus.NOT_FOUND),
    ACTIVITY_CANNOT_EDIT(1027, "Activity cannot be edited in current status", HttpStatus.BAD_REQUEST),
    ACTIVITY_CANNOT_DELETE(1028, "Activity cannot be deleted in current status", HttpStatus.BAD_REQUEST),
    ACTIVITY_INVALID_STATUS_TRANSITION(1029, "Invalid activity status transition", HttpStatus.BAD_REQUEST),
    ATTENDANCE_NOT_ALLOWED(1030, "Attendance is not allowed for this activity", HttpStatus.BAD_REQUEST),
    POINT_AWARD_NOT_ALLOWED(1031, "Points cannot be awarded for this activity", HttpStatus.BAD_REQUEST),
    ATTENDANCE_NOT_FOUND(1032, "Attendance not found", HttpStatus.NOT_FOUND),
    ACTIVITY_REPORT_NOT_ALLOWED(1033, "Report cannot be submitted for this activity", HttpStatus.BAD_REQUEST),
    ACTIVITY_FILE_NOT_FOUND(1034, "Activity file not found", HttpStatus.NOT_FOUND),
    ACTIVITY_REPORT_ALREADY_REVIEWED(1035, "Report has already been reviewed", HttpStatus.BAD_REQUEST),
    ROLE_ASSIGNMENT_FORBIDDEN(1036, "You are not allowed to perform this role assignment", HttpStatus.FORBIDDEN),
    CANNOT_MODIFY_ADMIN_USER(1037, "You cannot modify an administrator account", HttpStatus.FORBIDDEN),
    USER_IDENTITY_REQUIRED(1038, "Identity information is required for this account type", HttpStatus.BAD_REQUEST),
    ACTIVITY_REPORT_NOT_DOWNLOADED(1039, "Report must be downloaded before approval or rejection", HttpStatus.BAD_REQUEST),
    STUDENT_ACTIVITY_TIME_CONFLICT(1040, "Student already has another activity at this time", HttpStatus.BAD_REQUEST),
    ORGANIZER_ACTIVITY_TIME_CONFLICT(1041, "Organizer already has another activity at this time", HttpStatus.BAD_REQUEST);


    private int code;
    private String message;
    private HttpStatusCode httpStatus;

    ErrorCode(int code, String message, HttpStatusCode httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

}
