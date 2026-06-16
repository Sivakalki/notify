package com.notify.backend.upload;

import com.notify.backend.dto.upload.UserRow;
import com.notify.backend.upload.parser.ExcelFileParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ExcelFileParserTest {

    private final ExcelFileParser parser = new ExcelFileParser();

    @Test
    void parse_validXlsx_returnsAllValidRows() throws Exception {
        InputStream xlsx = buildXlsx(new String[][]{
                {"externalUserId", "email", "phone"},
                {"user1", "user1@example.com", "555-1111"},
                {"user2", "user2@example.com", "555-2222"}
        });

        List<UserRow> rows = parser.parse(xlsx);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("user1");
        assertThat(rows.get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(rows.get(1).getExternalUserId()).isEqualTo("user2");
    }

    @Test
    void parse_rowWithBlankExternalUserId_skipsRow() throws Exception {
        InputStream xlsx = buildXlsx(new String[][]{
                {"externalUserId", "email"},
                {"",              "no-id@example.com"},
                {"user1",         "user1@example.com"}
        });

        List<UserRow> rows = parser.parse(xlsx);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("user1");
    }

    @Test
    void parse_missingExternalUserIdColumn_throwsIllegalArgumentException() throws Exception {
        InputStream xlsx = buildXlsx(new String[][]{
                {"email", "phone"},
                {"user1@example.com", "555-1111"}
        });

        assertThatThrownBy(() -> parser.parse(xlsx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalUserId");
    }

    @Test
    void parse_dataOnlyNoHeader_returnsEmpty() throws Exception {
        // Workbook with no rows at all
        Workbook workbook = new XSSFWorkbook();
        workbook.createSheet("Users");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        List<UserRow> rows = parser.parse(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(rows).isEmpty();
    }

    private InputStream buildXlsx(String[][] data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    row.createCell(c).setCellValue(data[r][c]);
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return new ByteArrayInputStream(bos.toByteArray());
        }
    }
}