package org.floenergy;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.floenergy.model.entity.MeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class Nem12CsvProcessor {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final int MINUTES_IN_DAY = 24 * 60; // 1440

    private static final int FILE_LINES_LIMIT = 1000000;
    private static final Logger logger = LoggerFactory.getLogger(Nem12CsvProcessor.class);

    public static void processFile(String filePath,
                                   String outputFolder,
                                   String currentTime) {
        Instant start = Instant.now();

        try {
            readAndGenerateReadings(filePath, outputFolder, currentTime);
        } catch (Exception e) {
            logger.error("Failed to process file", e);
        } finally {
            Instant end = Instant.now();
            logger.info("Processing executed in {} seconds", Duration.between(start, end).toSeconds());
        }
    }

    private static void writeToFile(int fileIndex,
                                    String currentTime,
                                    List<MeterReading> readings,
                                    String outputFolder) throws IOException {
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

    // TODO should validate NEM12 format of CSV file?
    public static void readAndGenerateReadings(String filePath,
                                               String outputFolder,
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
                            if (readings.size() > FILE_LINES_LIMIT) {
                                writeToFile(fileIndex, currentTime, readings, outputFolder);
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
                writeToFile(fileIndex, currentTime, readings, outputFolder);
            }
        }
    }

    private static List<MeterReading> generate300readings(String currentNmi, String[] row, int intervalLengthInMins) {
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
}
