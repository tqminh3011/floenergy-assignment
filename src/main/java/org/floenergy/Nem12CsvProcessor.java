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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Nem12CsvProcessor {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final int MINUTES_IN_DAY = 24 * 60; // 1440
    public static final String OUTPUT_FOLDER = "output/";

    private static final int CHUNK_SIZE = 1000000;
    private static final Logger logger = LoggerFactory.getLogger(Nem12CsvProcessor.class);

    public static void processFile(String filePath) throws IOException {
        Instant start = Instant.now();

        try {
            List<MeterReading> meterReadings = readAndGenerateReadings(filePath);
            writeToFileInChunks(meterReadings);
        } catch (Exception e) {
            logger.error(e.toString());
            logger.error(Arrays.toString(e.getStackTrace()));
        } finally {
            Instant end = Instant.now();
            logger.info("Processing done in {} seconds", Duration.between(start, end).toSeconds());
        }
    }

    public static void writeToFileInChunks(List<MeterReading> readings) throws IOException {
        logger.info("Started writing to file in chunks");
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'-'HHmmss"));

        for (int i = 0; i < readings.size(); i += CHUNK_SIZE) {
            List<MeterReading> chunk = readings.subList(i, Math.min(i + CHUNK_SIZE, readings.size()));
            int index = i / CHUNK_SIZE;

            // Process the chunk
            logger.info("Processing chunk index: {}", index);

            String fileName = OUTPUT_FOLDER + "insert_statements_" + currentTime + "-" + index + ".sql";
            writeToFile(fileName, chunk);
        }
    }

    private static void writeToFile(String fileName, List<MeterReading> readings) throws IOException {
        StringBuilder sb = new StringBuilder();

        readings.forEach(reading -> {
            sb.append(
                    String.format("INSERT INTO meter_readings (nmi, timestamp, consumption) VALUES ('%s', '%s', %s);\n",
                            reading.nmi(),
                            reading.timestamp(),
                            reading.consumption()
                    )
            );
        });

        Files.createDirectories(Path.of(OUTPUT_FOLDER));

        Files.writeString(
                Path.of(fileName),
                sb.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        logger.info("Generated INSERT statements in file {}", fileName);
    }

    // TODO should validate NEM12 format of CSV file
    public static List<MeterReading> readAndGenerateReadings(String filePath) throws IOException, CsvValidationException {
        Path path = Path.of(filePath);
        logger.info("Start reading file {}", path.getFileName());

        List<MeterReading> readings = new ArrayList<>();

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
                            currentNmi = row[1];
                            intervalLengthInMins = Integer.parseInt(row[8]);
                        }

                        case "300" -> {
                            readings.addAll(generate300readings(currentNmi, row, intervalLengthInMins));
                        }
                    }
                }
            }
        }

        return readings;
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
