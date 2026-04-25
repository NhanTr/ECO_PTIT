package com.example.manage_activities.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.stereotype.Service;

import com.example.manage_activities.dto.request.UserCSVRequest;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.nio.charset.StandardCharsets;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CsvService {
    
    public List<UserCSVRequest> parseCSV(InputStream is) {
        try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            CsvToBean<UserCSVRequest> csvToBean = new CsvToBeanBuilder<UserCSVRequest>(reader)
                    .withType(UserCSVRequest.class)
                    .withSeparator(';')
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            return csvToBean.parse();
        } catch (IOException e) {
            log.error("Failed to parse CSV file", e);
            throw new IllegalStateException("Unable to parse CSV file", e);
        }
    }
}
