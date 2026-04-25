package com.example.manage_activities.dto.request;



import com.opencsv.bean.CsvBindByName;

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
public class UserCSVRequest {

    @CsvBindByName(column = "Họ và tên")
    String fullName;

    @CsvBindByName(column = "Ngày sinh")
    String dateOfBirth;

    @CsvBindByName(column = "Mã số sinh viên")
    String StudentId;

    @CsvBindByName(column = "Lớp")
    String classId;

    @CsvBindByName(column = "email")
    String email;

    @CsvBindByName(column = "Số điện thoại")
    String phone;
}
