package com.tierforge.app.upload;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvJobParser {

    private static final List<String> EXPECTED_HEADER =
            List.of("store_id", "store_name", "address", "city", "state", "country");

    private CsvJobParser() {
    }

    public static ParsedCsv parse(Reader reader) throws IOException {
        List<CSVRecord> records;
        try (CSVParser parser = CSVFormat.DEFAULT.builder().build().parse(reader)) {
            records = parser.getRecords();
        }

        if (records.isEmpty()) {
            throw new CsvValidationException(List.of("CSV file is empty"));
        }

        List<String> actualHeader = new ArrayList<>();
        records.get(0).forEach(actualHeader::add);
        if (!actualHeader.equals(EXPECTED_HEADER)) {
            throw new CsvValidationException(List.of("Invalid header. Expected: "
                    + String.join(",", EXPECTED_HEADER) + " but got: " + String.join(",", actualHeader)));
        }

        List<String> errors = new ArrayList<>();
        List<CsvStoreRow> rows = new ArrayList<>();
        Map<String, Integer> firstSeenLineByStoreId = new HashMap<>();

        for (int i = 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            int lineNumber = i + 1;

            if (record.size() != EXPECTED_HEADER.size()) {
                errors.add("Line " + lineNumber + ": expected " + EXPECTED_HEADER.size()
                        + " columns, found " + record.size());
                continue;
            }

            String storeId = record.get(0).trim();
            if (storeId.isEmpty()) {
                errors.add("Line " + lineNumber + ": store_id is required");
                continue;
            }

            Integer firstLine = firstSeenLineByStoreId.get(storeId);
            if (firstLine != null) {
                errors.add("Line " + lineNumber + ": duplicate store_id '" + storeId
                        + "' (first seen at line " + firstLine + ")");
                continue;
            }
            firstSeenLineByStoreId.put(storeId, lineNumber);

            rows.add(new CsvStoreRow(
                    storeId,
                    record.get(1).trim(),
                    record.get(2).trim(),
                    record.get(3).trim(),
                    record.get(4).trim(),
                    record.get(5).trim()));
        }

        if (!errors.isEmpty()) {
            throw new CsvValidationException(errors);
        }

        if (rows.isEmpty()) {
            throw new CsvValidationException(List.of("CSV contains no data rows"));
        }

        return new ParsedCsv(rows);
    }
}
