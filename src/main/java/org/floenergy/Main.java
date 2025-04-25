package org.floenergy;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Please enter CSV file path:");
        Scanner scanner = new Scanner(System.in);
        String csvPath = scanner.nextLine();

        Nem12CsvProcessor.processFile(csvPath);
    }
}