package org.floenergy;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.annotation.PostConstruct;
import org.floenergy.model.entity.MeterReading;
import org.floenergy.model.factory.MeterReadingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class Nem12CsvProcessor {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final int MINUTES_IN_DAY = 24 * 60; // 1440

    private static final Logger logger = LoggerFactory.getLogger(Nem12CsvProcessor.class);

    // exposed to config props to that it can be easily modified
    private final Integer fileLinesLimit;
    private final String outputFolder;

    public Nem12CsvProcessor(@Value("${app.file-lines-limit}") Integer fileLinesLimit,
                             @Value("${app.output-folder}") String outputFolder) {
        this.fileLinesLimit = fileLinesLimit;
        this.outputFolder = outputFolder;
    }

    @PostConstruct
    public void init() {
        logger.info("Configs: file-lines-limit={}, output-folder={}", fileLinesLimit, outputFolder);
    }

    public void processFile(String filePath,
                            String currentTime) {
        Instant start = Instant.now();

        try {
            readAndGenerateReadings(filePath, currentTime);
        } catch (Exception e) {
            logger.error("Failed to process file", e);
        } finally {
            Instant end = Instant.now();
            logger.info("Processing executed in {} seconds", Duration.between(start, end).toSeconds());
        }
    }

    // TODO should validate NEM12 format of CSV file?
    public void readAndGenerateReadings(String filePath,
                                        String currentTime) throws IOException, CsvValidationException {
        Path path = Path.of(filePath);
        logger.info("Start reading file {}", path.getFileName());

        List<MeterReading> readings = new ArrayList<>();
        int fileIndex = 0;

        try (Reader reader = Files.newBufferedReader(path)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                String[] row;
                String currentNmi = "";
                int intervalLengthInMins = 0;

                while ((row = csvReader.readNext()) != null) {
                    if (row.length == 0) continue;

                    String recordIndicator = row[0];
                    switch (recordIndicator) {
                        case "200" -> {
                            // check and write previous readings into file if limit exceeds
                            if (readings.size() > fileLinesLimit) {
                                writeToFile(fileIndex, currentTime, readings);
                                readings.clear();
                                fileIndex++;
                            }

                            currentNmi = row[1];
                            intervalLengthInMins = Integer.parseInt(row[8]);
                        }

                        case "300" -> readings.addAll(generate300readings(currentNmi, row, intervalLengthInMins));
                    }
                }

                // write last readings
                writeToFile(fileIndex, currentTime, readings);
            }
        }
    }

    private List<MeterReading> generate300readings(String currentNmi, String[] row, int intervalLengthInMins) {
        LocalDate startDate = LocalDate.parse(row[1], DATE_FORMATTER);
        int lastConsumptionIndex = MINUTES_IN_DAY / intervalLengthInMins + 1;
        List<MeterReading> currentReadings = new ArrayList<>();

        for (int index = 2; index <= lastConsumptionIndex; index++) {
            double consumption = Double.parseDouble(row[index]);

            MeterReading meterReading = MeterReadingFactory.createMeterReading(
                    currentNmi,
                    intervalLengthInMins,
                    index - 1,
                    startDate,
                    consumption
            );
            currentReadings.add(meterReading);
        }

        return currentReadings;
    }

    private void writeToFile(int fileIndex,
                             String currentTime,
                             List<MeterReading> readings) throws IOException {
        StringBuilder sb = new StringBuilder();
        String fileName = outputFolder + "insert_statements_" + currentTime + "-" + fileIndex + ".sql";

        logger.info("Writing to {}", fileName);

        readings.forEach(reading -> sb.append(
                String.format("INSERT INTO meter_readings (nmi, timestamp, consumption) VALUES ('%s', '%s', %s);\n",
                        reading.nmi(),
                        reading.timestamp(),
                        reading.consumption()
                )
        ));

        Files.createDirectories(Path.of(outputFolder));

        Files.writeString(
                Path.of(fileName),
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        logger.info("Generated INSERT statements in file {}", fileName);
    }
}
