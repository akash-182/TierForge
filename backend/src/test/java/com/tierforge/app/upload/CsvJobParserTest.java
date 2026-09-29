package com.tierforge.app.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class CsvJobParserTest {

    @Test
    void parsesValidCsvIntoRows() throws IOException {
        String csv = "store_id,store_name,address,city,state,country\n"
                + "ST000001,Fresh Supermarket #1,71 Church Street,New Delhi,Delhi,India\n"
                + "ST000002,City Pharmacy #2,27 Hill Street,New Delhi,Delhi,India\n";

        ParsedCsv result = CsvJobParser.parse(new StringReader(csv));

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).storeId()).isEqualTo("ST000001");
        assertThat(result.rows().get(0).storeName()).isEqualTo("Fresh Supermarket #1");
        assertThat(result.rows().get(1).storeId()).isEqualTo("ST000002");
    }

    @Test
    void rejectsMissingHeader() {
        String csv = "store_id,store_name,address,city,state\n"
                + "ST000001,Fresh Supermarket #1,71 Church Street,New Delhi,Delhi\n";

        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader(csv)))
                .isInstanceOf(CsvValidationException.class)
                .satisfies(ex -> assertThat(((CsvValidationException) ex).getErrors())
                        .anyMatch(msg -> msg.contains("Invalid header")));
    }

    @Test
    void rejectsReorderedHeader() {
        String csv = "store_name,store_id,address,city,state,country\n"
                + "Fresh Supermarket #1,ST000001,71 Church Street,New Delhi,Delhi,India\n";

        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader(csv)))
                .isInstanceOf(CsvValidationException.class);
    }

    @Test
    void rejectsEmptyStoreId() {
        String csv = "store_id,store_name,address,city,state,country\n"
                + ",Fresh Supermarket #1,71 Church Street,New Delhi,Delhi,India\n";

        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader(csv)))
                .isInstanceOf(CsvValidationException.class)
                .satisfies(ex -> assertThat(((CsvValidationException) ex).getErrors())
                        .anyMatch(msg -> msg.contains("Line 2") && msg.contains("store_id is required")));
    }

    @Test
    void rejectsDuplicateStoreId() {
        String csv = "store_id,store_name,address,city,state,country\n"
                + "ST000001,Fresh Supermarket #1,71 Church Street,New Delhi,Delhi,India\n"
                + "ST000001,Duplicate,9 Main Road,Mumbai,Maharashtra,India\n";

        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader(csv)))
                .isInstanceOf(CsvValidationException.class)
                .satisfies(ex -> assertThat(((CsvValidationException) ex).getErrors())
                        .anyMatch(msg -> msg.contains("Line 3") && msg.contains("duplicate store_id")));
    }

    @Test
    void collectsMultipleRowErrorsInOnePass() {
        String csv = "store_id,store_name,address,city,state,country\n"
                + ",Missing Id,71 Church Street,New Delhi,Delhi,India\n"
                + "ST000001,Fresh Supermarket #1,71 Church Street,New Delhi,Delhi,India\n"
                + "ST000001,Duplicate,9 Main Road,Mumbai,Maharashtra,India\n";

        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader(csv)))
                .isInstanceOf(CsvValidationException.class)
                .satisfies(ex -> assertThat(((CsvValidationException) ex).getErrors()).hasSize(2));
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader("")))
                .isInstanceOf(CsvValidationException.class)
                .satisfies(ex -> assertThat(((CsvValidationException) ex).getErrors())
                        .anyMatch(msg -> msg.contains("empty")));
    }

    @Test
    void rejectsHeaderOnlyFileWithNoDataRows() {
        String csv = "store_id,store_name,address,city,state,country\n";

        assertThatThrownBy(() -> CsvJobParser.parse(new StringReader(csv)))
                .isInstanceOf(CsvValidationException.class)
                .satisfies(ex -> assertThat(((CsvValidationException) ex).getErrors())
                        .anyMatch(msg -> msg.contains("no data rows")));
    }
}
