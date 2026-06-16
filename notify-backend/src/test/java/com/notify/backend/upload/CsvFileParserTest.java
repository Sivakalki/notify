package com.notify.backend.upload;

import com.notify.backend.dto.upload.UserRow;
import com.notify.backend.upload.parser.CsvFileParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFileParserTest {

    private final CsvFileParser parser = new CsvFileParser();

    @Test
    void parse_validCsv_returnsAllValidRows() throws Exception {
        String csv = "externalUserId,email,phone\n" +
                     "user1,user1@example.com,555-1111\n" +
                     "user2,user2@example.com,555-2222\n";

        List<UserRow> rows = parser.parse(stream(csv));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("user1");
        assertThat(rows.get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(rows.get(0).getPhone()).isEqualTo("555-1111");
        assertThat(rows.get(1).getExternalUserId()).isEqualTo("user2");
    }

    @Test
    void parse_rowMissingExternalUserId_skipsRow() throws Exception {
        String csv = "externalUserId,email,phone\n" +
                     ",no-id@example.com,555-0000\n" +
                     "user1,user1@example.com,555-1111\n";

        List<UserRow> rows = parser.parse(stream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("user1");
    }

    @Test
    void parse_headerOnlyCsv_returnsEmptyList() throws Exception {
        String csv = "externalUserId,email,phone\n";

        List<UserRow> rows = parser.parse(stream(csv));

        assertThat(rows).isEmpty();
    }

    @Test
    void parse_optionalColumnsAbsent_setsNullForMissingFields() throws Exception {
        String csv = "externalUserId\nuser1\n";

        List<UserRow> rows = parser.parse(stream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEmail()).isNull();
        assertThat(rows.get(0).getPhone()).isNull();
    }

    @Test
    void parse_blankOptionalField_setsNull() throws Exception {
        String csv = "externalUserId,email,phone\n" +
                     "user1,,\n";

        List<UserRow> rows = parser.parse(stream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEmail()).isNull();
        assertThat(rows.get(0).getPhone()).isNull();
    }

    private InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}