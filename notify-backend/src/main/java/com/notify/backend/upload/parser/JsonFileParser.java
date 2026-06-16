package com.notify.backend.upload.parser;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.notify.backend.dto.upload.UserRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonFileParser implements FileParser {

    private final JsonMapper objectMapper;

    @Override
    public List<UserRow> parse(InputStream inputStream) throws Exception {
        List<UserRow> rows = objectMapper.readValue(inputStream, new TypeReference<List<UserRow>>() {});
        List<UserRow> valid = rows.stream()
                .filter(r -> r.getExternalUserId() != null && !r.getExternalUserId().isBlank())
                .toList();
        log.info("JSON parsed: {} valid rows (skipped {})", valid.size(), rows.size() - valid.size());
        return valid;
    }
}
