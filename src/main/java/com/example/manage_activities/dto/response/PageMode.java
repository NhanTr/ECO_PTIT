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
public class PageMode<T> {
    List<T> content;           // Danh sách items
    Integer pageNumber;        // Số trang (0-based)
    Integer pageSize;          // Số items trên trang
    Long totalElements;        // Tổng số items
    Integer totalPages;        // Tổng số trang
    Boolean isFirst;           // Có phải trang đầu tiên?
    Boolean isLast;            // Có phải trang cuối cùng?
    Boolean isEmpty;           // Trang có trống không?
}
