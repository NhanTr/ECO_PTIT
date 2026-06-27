package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.SystemStatisticsResponse;
import com.example.manage_activities.entity.AcademicPeriod;
import com.example.manage_activities.repository.AcademicPeriodRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;
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
    AcademicPeriodRepository academicPeriodRepository;

    /**
     * Lấy thống kê cho học kỳ đang active (status=OPEN). Nếu không có, trả về tổng toàn hệ thống.
     * Hỗ trợ query thêm breakdown theo học kỳ trước để so sánh tăng/giảm.
     */
    public SystemStatisticsResponse getStatistics() {
        long totalEarnedPoints = attendanceRepository.findAll().stream()
                .map(attendance -> attendance.getEarnedPoints() == null ? 0 : attendance.getEarnedPoints())
                .mapToLong(Integer::longValue)
                .sum();

        SystemStatisticsResponse.SystemStatisticsResponseBuilder builder = SystemStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .totalActivities(activityRepository.count())
                .totalRegistrations(registrationRepository.count())
                .totalAttendance(attendanceRepository.count())
                .totalEarnedPoints(totalEarnedPoints);

        Optional<AcademicPeriod> activeOpt = academicPeriodRepository.findByStatus("OPEN").stream().findFirst();
        if (activeOpt.isPresent()) {
            AcademicPeriod period = activeOpt.get();
            builder
                    .periodLabel(formatPeriodLabel(period))
                    .periodActivities(activityRepository.countActivitiesInPeriod(period.getStartDate(), period.getEndDate()))
                    .periodRegistrations(registrationRepository.countRegistrationsInPeriod(period.getStartDate(), period.getEndDate()))
                    .periodAttendance(attendanceRepository.countPresentInPeriod(period.getStartDate(), period.getEndDate()))
                    .periodEarnedPoints(attendanceRepository.sumEarnedPointsInPeriod(period.getStartDate(), period.getEndDate()));

            // Kỳ trước: cùng loại học kỳ nhưng khác năm học, hoặc đơn giản hơn: học kỳ liền trước
            Optional<AcademicPeriod> previous = findPreviousPeriod(period);
            if (previous.isPresent()) {
                AcademicPeriod prev = previous.get();
                builder
                        .previousPeriodActivities(activityRepository.countActivitiesInPeriod(prev.getStartDate(), prev.getEndDate()))
                        .previousPeriodRegistrations(registrationRepository.countRegistrationsInPeriod(prev.getStartDate(), prev.getEndDate()))
                        .previousPeriodAttendance(attendanceRepository.countPresentInPeriod(prev.getStartDate(), prev.getEndDate()))
                        .previousPeriodEarnedPoints(attendanceRepository.sumEarnedPointsInPeriod(prev.getStartDate(), prev.getEndDate()));
            }
        }

        return builder.build();
    }

    /**
     * Tìm học kỳ trước theo thứ tự ưu tiên:
     * 1. Cùng năm học nhưng semester nhỏ hơn
     * 2. Năm học trước và semester=2
     */
    private Optional<AcademicPeriod> findPreviousPeriod(AcademicPeriod current) {
        if (current.getSemester() != null && current.getSemester() > 1) {
            return academicPeriodRepository.findByAcademicYearAndSemester(current.getAcademicYear(), current.getSemester() - 1);
        }
        // Tìm năm học trước đó: format "YYYY-YYYY", lấy năm bắt đầu giảm đi 1
        if (current.getAcademicYear() != null && current.getAcademicYear().contains("-")) {
            try {
                int startYear = Integer.parseInt(current.getAcademicYear().split("-")[0].trim());
                String prevYear = (startYear - 1) + "-" + startYear;
                return academicPeriodRepository.findByAcademicYearAndSemester(prevYear, 2);
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private String formatPeriodLabel(AcademicPeriod period) {
        return "HK" + (period.getSemester() == null ? "?" : period.getSemester()) + " " + period.getAcademicYear();
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
        StringBuilder content = new StringBuilder("System Statistics\n");
        content.append("Total users: ").append(statistics.getTotalUsers()).append("\n");
        content.append("Total activities: ").append(statistics.getTotalActivities()).append("\n");
        content.append("Total registrations: ").append(statistics.getTotalRegistrations()).append("\n");
        content.append("Total attendance: ").append(statistics.getTotalAttendance()).append("\n");
        content.append("Total earned points: ").append(statistics.getTotalEarnedPoints()).append("\n");

        if (statistics.getPeriodLabel() != null) {
            content.append("\n== ").append(statistics.getPeriodLabel()).append(" ==\n");
            content.append("Activities in period: ").append(statistics.getPeriodActivities() == null ? 0 : statistics.getPeriodActivities()).append("\n");
            content.append("Registrations in period: ").append(statistics.getPeriodRegistrations() == null ? 0 : statistics.getPeriodRegistrations()).append("\n");
            content.append("Attendance in period: ").append(statistics.getPeriodAttendance() == null ? 0 : statistics.getPeriodAttendance()).append("\n");
            content.append("Points in period: ").append(statistics.getPeriodEarnedPoints() == null ? 0 : statistics.getPeriodEarnedPoints()).append("\n");
        }

        return buildSimplePdf(content.toString());
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
                    <row r="7"><c r="A7" t="inlineStr"><is><t>Period (%s)</t></is></c><c r="B7" t="inlineStr"><is><t></t></is></c></row>
                    <row r="8"><c r="A8" t="inlineStr"><is><t>Activities in period</t></is></c><c r="B8"><v>%d</v></c></row>
                    <row r="9"><c r="A9" t="inlineStr"><is><t>Registrations in period</t></is></c><c r="B9"><v>%d</v></c></row>
                    <row r="10"><c r="A10" t="inlineStr"><is><t>Attendance in period</t></is></c><c r="B10"><v>%d</v></c></row>
                    <row r="11"><c r="A11" t="inlineStr"><is><t>Points in period</t></is></c><c r="B11"><v>%d</v></c></row>
                  </sheetData>
                </worksheet>
                """.formatted(
                statistics.getTotalUsers(),
                statistics.getTotalActivities(),
                statistics.getTotalRegistrations(),
                statistics.getTotalAttendance(),
                statistics.getTotalEarnedPoints(),
                statistics.getPeriodLabel() == null ? "all" : statistics.getPeriodLabel(),
                statistics.getPeriodActivities() == null ? 0 : statistics.getPeriodActivities(),
                statistics.getPeriodRegistrations() == null ? 0 : statistics.getPeriodRegistrations(),
                statistics.getPeriodAttendance() == null ? 0 : statistics.getPeriodAttendance(),
                statistics.getPeriodEarnedPoints() == null ? 0 : statistics.getPeriodEarnedPoints());
    }

    private void writeZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private byte[] buildSimplePdf(String text) {
        String escapedText = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\n", ") Tj T* (");
        String stream = "BT /F1 12 Tf 50 760 Td (" + escapedText + ") Tj ET";
        String pdf = "%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n" +
                "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
                "5 0 obj << /Length " + stream.length() + " >> stream\n" +
                stream + "\n" +
                "endstream endobj\n" +
                "trailer << /Root 1 0 R >>\n" +
                "%%EOF\n";
        return pdf.getBytes(StandardCharsets.UTF_8);
    }
}