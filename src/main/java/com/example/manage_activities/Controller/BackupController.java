package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.BackupRestoreRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.BackupFileResponse;
import com.example.manage_activities.service.BackupService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/backups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    BackupService backupService;

    @PostMapping("/export")
    public APIResponse<BackupFileResponse> exportBackup() {
        return APIResponse.<BackupFileResponse>builder()
                .message("Da tao ban sao luu")
                .result(backupService.createManualBackup())
                .build();
    }

    @GetMapping
    public APIResponse<List<BackupFileResponse>> listBackups() {
        return APIResponse.<List<BackupFileResponse>>builder()
                .result(backupService.listBackups())
                .build();
    }

    @PostMapping("/restore")
    public APIResponse<Void> restoreBackup(@Valid @RequestBody BackupRestoreRequest request) {
        backupService.restoreBackup(request.getFileName(), request.getConfirmation());
        return APIResponse.<Void>builder()
                .message("Da phuc hoi du lieu tu ban sao luu")
                .build();
    }
}
