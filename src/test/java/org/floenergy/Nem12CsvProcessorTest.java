package org.floenergy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Nem12CsvProcessorTest {
    @Test
    void When_process_Given_valid_Nem12_file_then_should_generate_correct_SQLs() throws IOException {
        String filePath = "src/test/resources/input/example.csv";
        String outputFolder = "src/test/resources/output/";
        String currentTime = "19961130-000000"; // hard-coded for testing

        Nem12CsvProcessor.processFile(filePath, outputFolder, currentTime);

        // Verify generated SQL file
        String expectedStatements = Files.readString(Path.of("src/test/resources/expected/insert_statements.sql"));
        String actualStatements = Files.readString(Path.of("src/test/resources/output/insert_statements_19961130-000000-0.sql"));

        assertEquals(expectedStatements, actualStatements);
    }
}