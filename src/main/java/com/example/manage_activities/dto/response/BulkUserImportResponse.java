package com.example.manage_activities.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulkUserImportResponse {

    /** Tổng số dòng dữ liệu hợp lệ đã xử lý. */
    int totalRows;

    /** Số user tạo thành công. */
    int successCount;

    /** Số dòng bị bỏ qua do lỗi validate hoặc xung đột dữ liệu. */
    int failureCount;

    /** Danh sách lỗi theo từng dòng (rowNumber đếm từ 2 vì row 1 là header). */
    List<RowError> errors;

    /** Username của các user tạo thành công (để admin thông báo lại). */
    List<String> createdUsernames;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RowError {
        int rowNumber;
        String username;
        String reason;
    }
}
