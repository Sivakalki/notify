package com.notify.backend.upload.parser;

import com.notify.backend.dto.upload.UserRow;

import java.io.InputStream;
import java.util.List;

public interface FileParser {
    List<UserRow> parse(InputStream inputStream) throws Exception;
}
