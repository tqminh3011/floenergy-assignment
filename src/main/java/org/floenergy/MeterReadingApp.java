package org.floenergy;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

@SpringBootApplication
public class MeterReadingApp implements CommandLineRunner {

    private final Nem12CsvProcessor nem12CsvProcessor;

    public MeterReadingApp(Nem12CsvProcessor nem12CsvProcessor) {
        this.nem12CsvProcessor = nem12CsvProcessor;
    }

    public static void main(String[] args) {
        SpringApplication.run(MeterReadingApp.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("Please enter CSV file path:");
        Scanner scanner = new Scanner(System.in);
        String csvPath = scanner.nextLine();

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'-'HHmmss"));
        nem12CsvProcessor.processFile(csvPath, currentTime);
    }
}