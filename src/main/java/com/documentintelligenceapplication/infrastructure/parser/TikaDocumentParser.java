package com.documentintelligenceapplication.infrastructure.parser;

import com.documentintelligenceapplication.presentation.exception.DocumentParseException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;

@Component
public class TikaDocumentParser implements DocumentParser {

    private final Tika tika = new Tika();

    @Override
    public String parse(File file) {
        try {
            return tika.parseToString(file);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse document using Tika: " + e.getMessage());
        } catch (Exception e) {
            throw new DocumentParseException("Unexpected parsing error: " + e.getMessage());
        }
    }
}
