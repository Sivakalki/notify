package com.notify.backend.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notify.backend.dto.upload.UserRow;
import com.notify.backend.upload.parser.JsonFileParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JsonFileParserTest {

    private final JsonFileParser parser = new JsonFileParser(new ObjectMapper());

    @Test
    void parse_validJson_returnsAllValidRows() throws Exception {
        String json = """
                [
                  {"externalUserId":"user1","email":"u1@x.com","phone":"111"},
                  {"externalUserId":"user2","email":"u2@x.com","phone":"222"}
                ]
                """;

        List<UserRow> rows = parser.parse(stream(json));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("user1");
        assertThat(rows.get(0).getEmail()).isEqualTo("u1@x.com");
        assertThat(rows.get(1).getExternalUserId()).isEqualTo("user2");
    }

    @Test
    void parse_rowWithBlankExternalUserId_isFiltered() throws Exception {
        String json = """
                [
                  {"externalUserId":"","email":"no-id@x.com"},
                  {"externalUserId":"user1","email":"u1@x.com"}
                ]
                """;

        List<UserRow> rows = parser.parse(stream(json));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("user1");
    }

    @Test
    void parse_rowWithNullExternalUserId_isFiltered() throws Exception {
        String json = """
                [
                  {"externalUserId":null,"email":"no-id@x.com"},
                  {"externalUserId":"user1"}
                ]
                """;

        List<UserRow> rows = parser.parse(stream(json));

        assertThat(rows).hasSize(1);
    }

    @Test
    void parse_emptyArray_returnsEmptyList() throws Exception {
        List<UserRow> rows = parser.parse(stream("[]"));
        assertThat(rows).isEmpty();
    }

    @Test
    void parse_unknownFields_areIgnored() throws Exception {
        String json = """
                [{"externalUserId":"u1","unknownField":"value"}]
                """;

        List<UserRow> rows = parser.parse(stream(json));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getExternalUserId()).isEqualTo("u1");
    }

    private InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}