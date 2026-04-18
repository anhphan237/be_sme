package com.sme.be_sme.modules.onboarding.service;

import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateCreateItem;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateCreateItem;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TaskLibraryExcelService {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.US);

    private static final int COL_CHECKLIST_NAME = 0;
    private static final int COL_CHECKLIST_STAGE = 1;
    private static final int COL_CHECKLIST_SORT_ORDER = 2;
    private static final int COL_CHECKLIST_STATUS = 3;
    private static final int COL_TASK_TITLE = 4;
    private static final int COL_TASK_DESCRIPTION = 5;
    private static final int COL_OWNER_TYPE = 6;
    private static final int COL_OWNER_REF_ID = 7;
    private static final int COL_DUE_DAYS_OFFSET = 8;
    private static final int COL_REQUIRE_ACK = 9;
    private static final int COL_REQUIRE_DOC = 10;
    private static final int COL_REQUIRES_MANAGER_APPROVAL = 11;
    private static final int COL_APPROVER_USER_ID = 12;
    private static final int COL_REQUIRED_DOCUMENT_IDS = 13;
    private static final int COL_TASK_SORT_ORDER = 14;
    private static final int COL_TASK_STATUS = 15;

    public byte[] buildTemplateWorkbook() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TaskLibrary");
            Row header = sheet.createRow(0);
            header.createCell(COL_CHECKLIST_NAME).setCellValue("Checklist Name");
            header.createCell(COL_CHECKLIST_STAGE).setCellValue("Checklist Stage");
            header.createCell(COL_CHECKLIST_SORT_ORDER).setCellValue("Checklist Sort Order");
            header.createCell(COL_CHECKLIST_STATUS).setCellValue("Checklist Status");
            header.createCell(COL_TASK_TITLE).setCellValue("Task Title");
            header.createCell(COL_TASK_DESCRIPTION).setCellValue("Task Description");
            header.createCell(COL_OWNER_TYPE).setCellValue("Owner Type");
            header.createCell(COL_OWNER_REF_ID).setCellValue("Owner Ref Id");
            header.createCell(COL_DUE_DAYS_OFFSET).setCellValue("Due Days Offset");
            header.createCell(COL_REQUIRE_ACK).setCellValue("Require Ack");
            header.createCell(COL_REQUIRE_DOC).setCellValue("Require Doc");
            header.createCell(COL_REQUIRES_MANAGER_APPROVAL).setCellValue("Requires Manager Approval");
            header.createCell(COL_APPROVER_USER_ID).setCellValue("Approver User Id");
            header.createCell(COL_REQUIRED_DOCUMENT_IDS).setCellValue("Required Document Ids");
            header.createCell(COL_TASK_SORT_ORDER).setCellValue("Task Sort Order");
            header.createCell(COL_TASK_STATUS).setCellValue("Task Status");

            Row sample = sheet.createRow(1);
            sample.createCell(COL_CHECKLIST_NAME).setCellValue("HR / Admin");
            sample.createCell(COL_CHECKLIST_STAGE).setCellValue("DAY1");
            sample.createCell(COL_CHECKLIST_SORT_ORDER).setCellValue("1");
            sample.createCell(COL_CHECKLIST_STATUS).setCellValue("ACTIVE");
            sample.createCell(COL_TASK_TITLE).setCellValue("Collect employee profile");
            sample.createCell(COL_TASK_DESCRIPTION).setCellValue("Collect tax code, bank account and ID card");
            sample.createCell(COL_OWNER_TYPE).setCellValue("DEPARTMENT");
            sample.createCell(COL_OWNER_REF_ID).setCellValue("department-id");
            sample.createCell(COL_DUE_DAYS_OFFSET).setCellValue("0");
            sample.createCell(COL_REQUIRE_ACK).setCellValue("false");
            sample.createCell(COL_REQUIRE_DOC).setCellValue("false");
            sample.createCell(COL_REQUIRES_MANAGER_APPROVAL).setCellValue("false");
            sample.createCell(COL_APPROVER_USER_ID).setCellValue("");
            sample.createCell(COL_REQUIRED_DOCUMENT_IDS).setCellValue("");
            sample.createCell(COL_TASK_SORT_ORDER).setCellValue("1");
            sample.createCell(COL_TASK_STATUS).setCellValue("ACTIVE");

            for (int i = 0; i <= COL_TASK_STATUS; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to build excel template");
        }
    }

    public ParsedTaskLibrary parseImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "file is required");
        }

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "excel file does not contain any sheet");
            }

            Map<String, ChecklistAccumulator> checklistByKey = new LinkedHashMap<>();
            int taskRowCount = 0;

            int firstDataRow = 1;
            for (int rowNum = firstDataRow; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                String checklistName = readText(row, COL_CHECKLIST_NAME);
                String taskTitle = readText(row, COL_TASK_TITLE);
                if (!StringUtils.hasText(checklistName)) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "row " + (rowNum + 1) + ": checklist name is required");
                }
                if (!StringUtils.hasText(taskTitle)) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "row " + (rowNum + 1) + ": task title is required");
                }

                String checklistStage = normalizeBlankToNull(readText(row, COL_CHECKLIST_STAGE));
                Integer checklistSort = parseInteger(row, COL_CHECKLIST_SORT_ORDER, "checklist sort order", rowNum);
                String checklistStatus = normalizeBlankToNull(readText(row, COL_CHECKLIST_STATUS));
                String checklistKey =
                        checklistName.trim() + "|" + safeKey(checklistStage) + "|" + safeKey(checklistSort) + "|" + safeKey(checklistStatus);

                ChecklistAccumulator checklist = checklistByKey.computeIfAbsent(checklistKey, ignored -> {
                    ChecklistAccumulator acc = new ChecklistAccumulator();
                    acc.name = checklistName.trim();
                    acc.stage = checklistStage;
                    acc.sortOrder = checklistSort;
                    acc.status = checklistStatus;
                    return acc;
                });

                TaskTemplateCreateItem task = new TaskTemplateCreateItem();
                task.setTitle(taskTitle.trim());
                task.setDescription(normalizeBlankToNull(readText(row, COL_TASK_DESCRIPTION)));
                task.setOwnerType(normalizeBlankToNull(readText(row, COL_OWNER_TYPE)));
                task.setOwnerRefId(normalizeBlankToNull(readText(row, COL_OWNER_REF_ID)));
                task.setDueDaysOffset(parseInteger(row, COL_DUE_DAYS_OFFSET, "due days offset", rowNum));
                task.setRequireAck(parseBoolean(row, COL_REQUIRE_ACK, "require ack", rowNum));
                task.setRequireDoc(parseBoolean(row, COL_REQUIRE_DOC, "require doc", rowNum));
                task.setRequiresManagerApproval(
                        parseBoolean(row, COL_REQUIRES_MANAGER_APPROVAL, "requires manager approval", rowNum));
                task.setApproverUserId(normalizeBlankToNull(readText(row, COL_APPROVER_USER_ID)));
                task.setRequiredDocumentIds(parseDocumentIds(readText(row, COL_REQUIRED_DOCUMENT_IDS)));
                task.setSortOrder(parseInteger(row, COL_TASK_SORT_ORDER, "task sort order", rowNum));
                task.setStatus(normalizeBlankToNull(readText(row, COL_TASK_STATUS)));
                checklist.tasks.add(task);
                taskRowCount++;
            }

            if (checklistByKey.isEmpty()) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "excel file does not contain any task row");
            }

            List<ChecklistTemplateCreateItem> checklists = new ArrayList<>();
            for (ChecklistAccumulator value : checklistByKey.values()) {
                ChecklistTemplateCreateItem checklist = new ChecklistTemplateCreateItem();
                checklist.setName(value.name);
                checklist.setStage(value.stage);
                checklist.setSortOrder(value.sortOrder);
                checklist.setStatus(value.status);
                checklist.setTasks(value.tasks);
                checklists.add(checklist);
            }

            ParsedTaskLibrary parsed = new ParsedTaskLibrary();
            parsed.setChecklists(checklists);
            parsed.setImportedTasks(taskRowCount);
            parsed.setTotalRows(sheet.getLastRowNum());
            return parsed;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "failed to parse excel file: " + e.getMessage());
        }
    }

    private static String safeKey(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String readText(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String value = DATA_FORMATTER.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static boolean isEmptyRow(Row row) {
        for (int i = COL_CHECKLIST_NAME; i <= COL_TASK_STATUS; i++) {
            if (StringUtils.hasText(readText(row, i))) {
                return false;
            }
        }
        return true;
    }

    private static Integer parseInteger(Row row, int column, String fieldName, int rowNum) {
        String text = readText(row, column);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "row " + (rowNum + 1) + ": invalid " + fieldName);
        }
    }

    private static Boolean parseBoolean(Row row, int column, String fieldName, int rowNum) {
        String text = readText(row, column);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "true", "1", "yes", "y" -> Boolean.TRUE;
            case "false", "0", "no", "n" -> Boolean.FALSE;
            default -> throw AppException.of(ErrorCodes.BAD_REQUEST, "row " + (rowNum + 1) + ": invalid " + fieldName);
        };
    }

    private static List<String> parseDocumentIds(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String[] pieces = text.split("[,;\\n]");
        Set<String> ids = new LinkedHashSet<>();
        for (String piece : pieces) {
            if (StringUtils.hasText(piece)) {
                ids.add(piece.trim());
            }
        }
        return List.copyOf(ids);
    }

    private static String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static class ChecklistAccumulator {
        private String name;
        private String stage;
        private Integer sortOrder;
        private String status;
        private final List<TaskTemplateCreateItem> tasks = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ParsedTaskLibrary {
        private List<ChecklistTemplateCreateItem> checklists;
        private int totalRows;
        private int importedTasks;
    }
}
