package com.notify.backend.upload.parser;

import com.notify.backend.dto.upload.UserRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CsvFileParser implements FileParser {

    @Override
    public List<UserRow> parse(InputStream inputStream) throws Exception {
        List<UserRow> rows = new ArrayList<>();
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                String externalUserId = record.isMapped("externalUserId")
                        ? record.get("externalUserId") : null;
                if (externalUserId == null || externalUserId.isBlank()) {
                    log.warn("Skipping CSV row {} — missing externalUserId", record.getRecordNumber());
                    continue;
                }
                rows.add(UserRow.builder()
                        .externalUserId(externalUserId)
                        .email(mapped(record, "email"))
                        .phone(mapped(record, "phone"))
                        .build());
            }
        }
        log.info("CSV parsed: {} valid rows", rows.size());
        return rows;
    }

    private String mapped(CSVRecord record, String column) {
        if (!record.isMapped(column)) return null;
        String value = record.get(column);
        return (value == null || value.isBlank()) ? null : value;
    }
}
