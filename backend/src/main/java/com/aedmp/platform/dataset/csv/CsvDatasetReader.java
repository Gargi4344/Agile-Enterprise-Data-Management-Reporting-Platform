package com.aedmp.platform.dataset.csv;

import com.aedmp.platform.common.exception.BadRequestException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvDatasetReader {

    public CsvTable read(InputStream inputStream) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headerRow = reader.readNext();
            if (headerRow == null || headerRow.length == 0) {
                throw new BadRequestException("MALFORMED_FILE", "File has no header row");
            }
            List<String> header = new ArrayList<>();
            for (String h : headerRow) {
                header.add(h.trim());
            }
            List<String[]> rows = new ArrayList<>();
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length == 1 && row[0].isBlank()) {
                    continue;
                }
                if (row.length != header.size()) {
                    throw new BadRequestException("MALFORMED_FILE",
                            "Row " + (rows.size() + 2) + " has " + row.length + " columns, expected " + header.size());
                }
                rows.add(row);
            }
            return new CsvTable(header, rows);
        } catch (IOException | CsvValidationException e) {
            throw new BadRequestException("MALFORMED_FILE", "Could not parse file as CSV: " + e.getMessage());
        }
    }
}
