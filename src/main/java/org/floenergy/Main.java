package org.floenergy;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main {
    public static final String OUTPUT_FOLDER = "output/";

    public static void main(String[] args) throws IOException {
        System.out.println("Please enter CSV file path:");
        Scanner scanner = new Scanner(System.in);
        String csvPath = scanner.nextLine();

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'-'HHmmss"));
        Nem12CsvProcessor.processFile(csvPath, OUTPUT_FOLDER, currentTime);
    }
}