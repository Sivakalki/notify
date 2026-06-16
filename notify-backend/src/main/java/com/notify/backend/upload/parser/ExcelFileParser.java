package com.notify.backend.upload.parser;

import com.notify.backend.dto.upload.UserRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExcelFileParser implements FileParser {

    @Override
    public List<UserRow> parse(InputStream inputStream) throws Exception {
        List<UserRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("Excel file has no header row");
                return rows;
            }

            Map<String, Integer> colIndex = buildHeaderIndex(headerRow);
            Integer externalUserIdCol = colIndex.get("externalUserId");
            if (externalUserIdCol == null) {
                throw new IllegalArgumentException("Excel file missing required column: externalUserId");
            }

            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String externalUserId = cellValue(row, externalUserIdCol, formatter);
                if (externalUserId == null || externalUserId.isBlank()) {
                    log.warn("Skipping Excel row {} — missing externalUserId", i + 1);
                    continue;
                }
                rows.add(UserRow.builder()
                        .externalUserId(externalUserId)
                        .email(colIndex.containsKey("email") ? cellValue(row, colIndex.get("email"), formatter) : null)
                        .phone(colIndex.containsKey("phone") ? cellValue(row, colIndex.get("phone"), formatter) : null)
                        .build());
            }
        }
        log.info("Excel parsed: {} valid rows", rows.size());
        return rows;
    }

    private Map<String, Integer> buildHeaderIndex(Row headerRow) {
        DataFormatter formatter = new DataFormatter();
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell).trim();
            if (!header.isBlank()) {
                index.put(header, cell.getColumnIndex());
            }
        }
        return index;
    }

    private String cellValue(Row row, int col, DataFormatter formatter) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }
}
