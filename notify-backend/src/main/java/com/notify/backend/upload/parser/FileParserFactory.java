package com.notify.backend.upload.parser;

import com.notify.backend.entity.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileParserFactory {

    private final CsvFileParser csvParser;
    private final ExcelFileParser excelParser;
    private final JsonFileParser jsonParser;

    public FileParser getParser(FileType fileType) {
        return switch (fileType) {
            case CSV   -> csvParser;
            case EXCEL -> excelParser;
            case JSON  -> jsonParser;
        };
    }
}
