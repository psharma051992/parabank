package com.fundtransfer.support;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the next customer from registration-data.xlsx.
 * The first row is the header. Each test run takes the following row,
 * then the one after that. After the last row it starts again at the first customer.
 * The username is letters only. The signup field rejects a name that contains digits,
 * and the message it shows does not say that. A letter ending keeps each run unique.
 */
public class RegistrationExcel {

    private static final String FILE = "registration-data.xlsx";
    private static final Path NEXT_ROW = Path.of("target", "next-registration-row.txt");

    public static Customer nextCustomer() {
        try (InputStream in = RegistrationExcel.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException("Missing src/test/resources/" + FILE);
            }
            try (Workbook workbook = WorkbookFactory.create(in)) {
                Sheet sheet = workbook.getSheetAt(0);
                int dataRows = sheet.getLastRowNum();
                if (dataRows < 1) {
                    throw new IllegalStateException(FILE + " has no customer rows");
                }
                Row row = sheet.getRow(nextIndex(dataRows) + 1);
                DataFormatter cells = new DataFormatter();
                String username = freshUsername(cells.formatCellValue(row.getCell(8)));
                return new Customer(
                        cells.formatCellValue(row.getCell(0)),
                        cells.formatCellValue(row.getCell(1)),
                        cells.formatCellValue(row.getCell(2)),
                        cells.formatCellValue(row.getCell(3)),
                        cells.formatCellValue(row.getCell(4)),
                        cells.formatCellValue(row.getCell(5)),
                        cells.formatCellValue(row.getCell(6)),
                        cells.formatCellValue(row.getCell(7)),
                        username,
                        cells.formatCellValue(row.getCell(9)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + FILE, ex);
        }
    }

    /** Letters only. Digits from the clock are written as a through j. */
    public static String freshUsername(String base) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (Character.isLetter(c)) {
                name.append(Character.toLowerCase(c));
            }
        }
        if (name.length() == 0) {
            name.append("user");
        }
        if (name.length() > 8) {
            name.setLength(8);
        }
        String digits = Long.toString(Math.floorMod(System.nanoTime(), 100000000L));
        for (int i = 0; i < digits.length(); i++) {
            name.append((char) ('a' + (digits.charAt(i) - '0')));
        }
        return name.toString();
    }

    private static int nextIndex(int dataRows) throws IOException {
        int index = 0;
        if (Files.exists(NEXT_ROW)) {
            String text = Files.readString(NEXT_ROW).trim();
            if (!text.isEmpty()) {
                index = Integer.parseInt(text);
            }
        }
        if (index < 0 || index >= dataRows) {
            index = 0;
        }
        Files.createDirectories(NEXT_ROW.getParent());
        Files.writeString(NEXT_ROW, Integer.toString(index + 1), StandardCharsets.UTF_8);
        return index;
    }
}
