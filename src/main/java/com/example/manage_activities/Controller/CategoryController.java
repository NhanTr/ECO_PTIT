package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.CategoryRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.CategoryResponse;
import com.example.manage_activities.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * QTHT #7 - Quản lý danh mục hệ thống.
 * Endpoint được bảo vệ bởi SecurityConfig (/api/admin/categories/**).
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    CategoryService categoryService;

    @GetMapping
    public APIResponse<List<CategoryResponse>> list(
            @RequestParam(required = false) String type) {
        return APIResponse.<List<CategoryResponse>>builder()
                .result(categoryService.getAll(type))
                .build();
    }

    @PostMapping
    public APIResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return APIResponse.<CategoryResponse>builder()
                .message("Đã tạo danh mục")
                .result(categoryService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<CategoryResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        return APIResponse.<CategoryResponse>builder()
                .message("Đã cập nhật danh mục")
                .result(categoryService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable String id) {
        categoryService.delete(id);
        return APIResponse.<Void>builder()
                .message("Đã vô hiệu hóa danh mục")
                .build();
    }
}