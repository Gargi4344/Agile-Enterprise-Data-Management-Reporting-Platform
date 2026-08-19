package com.aedmp.platform.dataset.csv;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CsvWriterUtil {

    public byte[] write(CsvTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", table.header())).append('\n');
        for (String[] row : table.rows()) {
            sb.append(String.join(",", row)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
