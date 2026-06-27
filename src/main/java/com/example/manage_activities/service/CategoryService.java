package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.CategoryRequest;
import com.example.manage_activities.dto.response.CategoryResponse;
import com.example.manage_activities.entity.Category;
import com.example.manage_activities.enums.CategoryType;
import com.example.manage_activities.enums.UserAccountStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.CategoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * QTHT #7 - Quản lý danh mục hệ thống (QTHT_QĐ 3, QTHT_BM 2).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(String type) {
        List<Category> categories = StringUtils.hasText(type)
                ? categoryRepository.findByType(type.toUpperCase())
                : categoryRepository.findAll();
        return categories.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String type = normalizeType(request.getType());
        if (categoryRepository.existsByTypeAndCode(type, request.getCode())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        Category category = Category.builder()
                .id(generateId())
                .type(type)
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(resolveStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        String type = normalizeType(request.getType());
        if (!category.getType().equals(type)
                || !category.getCode().equals(request.getCode())) {
            if (categoryRepository.existsByTypeAndCode(type, request.getCode())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }
        category.setType(type);
        category.setCode(request.getCode().trim());
        category.setName(request.getName().trim());
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getStatus())) {
            category.setStatus(UserAccountStatus.fromValue(request.getStatus()).getValue());
        }
        category.setUpdatedAt(LocalDateTime.now());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        // QTHT_QĐ 3: kiểm tra ràng buộc dữ liệu trước khi xóa.
        // Ở đây chỉ xóa mềm (đổi trạng thái inactive) thay vì xóa cứng
        // để tránh mất tham chiếu từ Activity/Profile.
        category.setStatus(UserAccountStatus.INACTIVE.getValue());
        category.setUpdatedAt(LocalDateTime.now());
        categoryRepository.save(category);
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type) || !CategoryType.isValid(type)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return type.toUpperCase();
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return UserAccountStatus.ACTIVE.getValue();
        }
        return UserAccountStatus.fromValue(status).getValue();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .type(category.getType())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}