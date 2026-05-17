package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.SystemStatisticsResponse;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemStatisticsService {

    UserRepository userRepository;
    ActivityRepository activityRepository;
    RegistrationRepository registrationRepository;
    AttendanceRepository attendanceRepository;

    public SystemStatisticsResponse getStatistics() {
        long totalEarnedPoints = attendanceRepository.findAll().stream()
                .map(attendance -> attendance.getEarnedPoints() == null ? 0 : attendance.getEarnedPoints())
                .mapToLong(Integer::longValue)
                .sum();

        return SystemStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .totalActivities(activityRepository.count())
                .totalRegistrations(registrationRepository.count())
                .totalAttendance(attendanceRepository.count())
                .totalEarnedPoints(totalEarnedPoints)
                .build();
    }

    public byte[] exportExcel() {
        SystemStatisticsResponse statistics = getStatistics();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                writeZipEntry(zip, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>
                        """);
                writeZipEntry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>
                        """);
                writeZipEntry(zip, "xl/workbook.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                          <sheets><sheet name="Statistics" sheetId="1" r:id="rId1"/></sheets>
                        </workbook>
                        """);
                writeZipEntry(zip, "xl/_rels/workbook.xml.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>
                        """);
                writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildSheetXml(statistics));
            }
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot export Excel statistics", exception);
        }
    }

    public byte[] exportPdf() {
        SystemStatisticsResponse statistics = getStatistics();
        String content = "System Statistics\\n"
                + "Total users: " + statistics.getTotalUsers() + "\\n"
                + "Total activities: " + statistics.getTotalActivities() + "\\n"
                + "Total registrations: " + statistics.getTotalRegistrations() + "\\n"
                + "Total attendance: " + statistics.getTotalAttendance() + "\\n"
                + "Total earned points: " + statistics.getTotalEarnedPoints();
        return buildSimplePdf(content);
    }

    private String buildSheetXml(SystemStatisticsResponse statistics) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1"><c r="A1" t="inlineStr"><is><t>Metric</t></is></c><c r="B1" t="inlineStr"><is><t>Value</t></is></c></row>
                    <row r="2"><c r="A2" t="inlineStr"><is><t>Total users</t></is></c><c r="B2"><v>%d</v></c></row>
                    <row r="3"><c r="A3" t="inlineStr"><is><t>Total activities</t></is></c><c r="B3"><v>%d</v></c></row>
                    <row r="4"><c r="A4" t="inlineStr"><is><t>Total registrations</t></is></c><c r="B4"><v>%d</v></c></row>
                    <row r="5"><c r="A5" t="inlineStr"><is><t>Total attendance</t></is></c><c r="B5"><v>%d</v></c></row>
                    <row r="6"><c r="A6" t="inlineStr"><is><t>Total earned points</t></is></c><c r="B6"><v>%d</v></c></row>
                  </sheetData>
                </worksheet>
                """.formatted(
                statistics.getTotalUsers(),
                statistics.getTotalActivities(),
                statistics.getTotalRegistrations(),
                statistics.getTotalAttendance(),
                statistics.getTotalEarnedPoints());
    }

    private void writeZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private byte[] buildSimplePdf(String text) {
        String escapedText = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\\n", ") Tj T* (");
        String stream = "BT /F1 12 Tf 50 760 Td (" + escapedText + ") Tj ET";
        String pdf = """
                %PDF-1.4
                1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
                2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
                3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj
                4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
                5 0 obj << /Length %d >> stream
                %s
                endstream endobj
                trailer << /Root 1 0 R >>
                %%EOF
                """.formatted(stream.length(), stream);
        return pdf.getBytes(StandardCharsets.UTF_8);
    }
}
