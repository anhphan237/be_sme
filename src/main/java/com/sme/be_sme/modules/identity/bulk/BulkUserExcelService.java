package com.sme.be_sme.modules.identity.bulk;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BulkUserExcelService {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.US);
    private static final DateTimeFormatter DATE_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int COL_EMAIL = 0;
    private static final int COL_FULL_NAME = 1;
    private static final int COL_ROLE_CODE = 2;
    private static final int COL_DEPARTMENT_NAME = 3;
    private static final int COL_EMPLOYEE_CODE = 4;
    private static final int COL_JOB_TITLE = 5;
    private static final int COL_MANAGER_EMAIL = 6;
    private static final int COL_START_DATE = 7;
    private static final int COL_WORK_LOCATION = 8;
    private static final int COL_PHONE = 9;

    public byte[] buildTemplateWorkbook() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");
            Row header = sheet.createRow(0);
            header.createCell(COL_EMAIL).setCellValue("Email*");
            header.createCell(COL_FULL_NAME).setCellValue("Full Name*");
            header.createCell(COL_ROLE_CODE).setCellValue("Role Code* (ADMIN|HR|MANAGER|IT|EMPLOYEE)");
            header.createCell(COL_DEPARTMENT_NAME).setCellValue("Department Name (required for MANAGER/EMPLOYEE)");
            header.createCell(COL_EMPLOYEE_CODE).setCellValue("Employee Code");
            header.createCell(COL_JOB_TITLE).setCellValue("Job Title");
            header.createCell(COL_MANAGER_EMAIL).setCellValue("Manager Email");
            header.createCell(COL_START_DATE).setCellValue("Start Date (yyyy-MM-dd or dd/MM/yyyy)");
            header.createCell(COL_WORK_LOCATION).setCellValue("Work Location");
            header.createCell(COL_PHONE).setCellValue("Phone");

            Row sample = sheet.createRow(1);
            sample.createCell(COL_EMAIL).setCellValue("new.employee@company.com");
            sample.createCell(COL_FULL_NAME).setCellValue("Nguyen Van A");
            sample.createCell(COL_ROLE_CODE).setCellValue("EMPLOYEE");
            sample.createCell(COL_DEPARTMENT_NAME).setCellValue("Engineering");
            sample.createCell(COL_EMPLOYEE_CODE).setCellValue("EMP-2026-001");
            sample.createCell(COL_JOB_TITLE).setCellValue("Software Engineer");
            sample.createCell(COL_MANAGER_EMAIL).setCellValue("manager@company.com");
            sample.createCell(COL_START_DATE).setCellValue("2026-04-20");
            sample.createCell(COL_WORK_LOCATION).setCellValue("HCM");
            sample.createCell(COL_PHONE).setCellValue("0900000000");

            for (int i = 0; i <= COL_PHONE; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to build bulk user excel template");
        }
    }

    public List<ParsedUserRow> parseImportFile(MultipartFile file, IdentityBulkUserImportProperties properties) {
        if (file == null || file.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "file is required");
        }

        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw AppException.of(ErrorCodes.LIMIT_EXCEEDED, "file exceeds max allowed size");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.toLowerCase(Locale.US).endsWith(".xlsx")) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only .xlsx files are supported");
        }

        // Protect against compressed bomb payloads in Office files.
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(Math.max(properties.getMaxFileSizeBytes() * 2, 4 * 1024 * 1024L));

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "excel file does not contain any sheet");
            }

            List<ParsedUserRow> rows = new ArrayList<>();
            int firstDataRow = 1;
            for (int rowNum = firstDataRow; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                if (rows.size() >= properties.getMaxRows()) {
                    throw AppException.of(
                            ErrorCodes.LIMIT_EXCEEDED,
                            "excel contains more than " + properties.getMaxRows() + " data rows");
                }

                ParsedUserRow parsed = new ParsedUserRow(rowNum + 1);
                CreateUserRequest request = new CreateUserRequest();
                request.setEmail(normalizeLowerEmail(readText(row, COL_EMAIL)));
                request.setFullName(normalizeBlankToNull(readText(row, COL_FULL_NAME)));
                request.setRoleCode(normalizeUpperCode(readText(row, COL_ROLE_CODE)));
                request.setDepartmentId(normalizeBlankToNull(readText(row, COL_DEPARTMENT_NAME)));
                request.setEmployeeCode(normalizeBlankToNull(readText(row, COL_EMPLOYEE_CODE)));
                request.setJobTitle(normalizeBlankToNull(readText(row, COL_JOB_TITLE)));
                request.setManagerUserId(normalizeBlankToNull(readText(row, COL_MANAGER_EMAIL)));
                request.setStartDate(parseDate(row, COL_START_DATE, parsed));
                request.setWorkLocation(normalizeBlankToNull(readText(row, COL_WORK_LOCATION)));
                request.setPhone(normalizeBlankToNull(readText(row, COL_PHONE)));
                parsed.setRequest(request);
                rows.add(parsed);
            }

            if (rows.isEmpty()) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "excel file does not contain any user row");
            }

            return rows;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "failed to parse excel file: " + e.getMessage());
        }
    }

    private static Date parseDate(Row row, int column, ParsedUserRow parsed) {
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }

        String text = readText(row, column);
        if (!StringUtils.hasText(text)) {
            return null;
        }

        try {
            LocalDate localDate = LocalDate.parse(text, DATE_YYYY_MM_DD);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignore) {
            // try alternate format below
        }

        try {
            LocalDate localDate = LocalDate.parse(text, DATE_DD_MM_YYYY);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            parsed.addError("invalid startDate format, expected yyyy-MM-dd or dd/MM/yyyy");
            return null;
        }
    }

    private static boolean isEmptyRow(Row row) {
        for (int i = COL_EMAIL; i <= COL_PHONE; i++) {
            if (StringUtils.hasText(readText(row, i))) {
                return false;
            }
        }
        return true;
    }

    private static String readText(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String value = DATA_FORMATTER.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizeUpperCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.US) : null;
    }

    private static String normalizeLowerEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.US) : null;
    }

    @Getter
    public static class ParsedUserRow {
        private final int rowNumber;
        private final List<String> errors = new ArrayList<>();
        private CreateUserRequest request;

        public ParsedUserRow(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public void setRequest(CreateUserRequest request) {
            this.request = request;
        }

        public void addError(String error) {
            if (StringUtils.hasText(error)) {
                errors.add(error);
            }
        }

        public boolean hasError() {
            return !errors.isEmpty();
        }
    }
}
