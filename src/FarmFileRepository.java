/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 8 - DMS Project Phase 2
 * Farm Finder
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;


public class FarmFileRepository {
    private final String delimiter;
    private final FarmValidator validator;
    private int skippedLines;
    private String lastError;

    public FarmFileRepository(String delimiter) {
        this.delimiter = delimiter;
        this.validator = new FarmValidator();
        this.skippedLines = 0;
        this.lastError = "";
    }

    /** lines skipping */
    public ArrayList<Farm> loadFarms(String path) {
        ArrayList<Farm> result = new ArrayList<>();
        skippedLines = 0;
        lastError = "";
        if (path == null || path.trim().isEmpty()) {
            lastError = "File name can't be blank.";
            return result;
        }
        try (BufferedReader reader = Files.newBufferedReader(
                Path.of(path.trim()), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    skippedLines++;
                    continue;
                }
                Farm farm = parseLine(line);
                if (farm == null) {
                    skippedLines++;
                } else {
                    result.add(farm);
                }
            }
        } catch (IOException | InvalidPathException | SecurityException exception) {
            lastError = "File could not be read: " + exception.getMessage();
            return new ArrayList<>();
        }
        return result;
    }

    /** separate all 8 fields */
    public Farm parseLine(String line) {
        if (line == null) {
            return null;
        }
        String[] fields = line.split(java.util.regex.Pattern.quote(delimiter), -1);
        if (fields.length != 8) {
            return null;
        }
        try {
            int id = Integer.parseInt(fields[0].trim());
            String name = fields[1].trim();
            String city = fields[2].trim();
            String products = fields[3].trim();
            String season = fields[4].trim();
            String organicText = fields[5].trim();

            // Boolean.parseBoolean alone would treat ANY misspelling as false.
            if (!organicText.equalsIgnoreCase("true")
                    && !organicText.equalsIgnoreCase("false")) {
                return null;
            }
            boolean organic = Boolean.parseBoolean(organicText);
            double miles = Double.parseDouble(fields[6].trim());
            LocalDate updated = LocalDate.parse(fields[7].trim());
            Farm farm = new Farm(id, name, city, products, season,
                    organic, miles, updated);
            return validator.isValidFarm(farm) ? farm : null;
        } catch (NumberFormatException | DateTimeParseException exception) {
            return null;
        }
    }

    /** return false instead of crashing */
    public boolean saveFarms(String path, ArrayList<Farm> farms) {
        lastError = "";
        if (path == null || path.trim().isEmpty()) {
            lastError = "File name can't be blank.";
            return false;
        }
        if (farms == null) {
            lastError = "There are no farm records to save.";
            return false;
        }
        for (Farm farm : farms) {
            if (!validator.isValidFarm(farm)) {
                lastError = "Cannot save an invalid farm record.";
                return false;
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(path.trim()), StandardCharsets.UTF_8)) {
            for (Farm farm : farms) {
                writer.write(formatLine(farm));
                writer.newLine();
            }
            return true;
        } catch (IOException | InvalidPathException | SecurityException exception) {
            lastError = "File could not be saved: " + exception.getMessage();
            return false;
        }
    }

    public String formatLine(Farm farm) {
        return farm.getFarmId() + delimiter + farm.getName() + delimiter
                + farm.getCity() + delimiter + farm.getProducts() + delimiter
                + farm.getSeason() + delimiter + farm.isOrganic() + delimiter
                + farm.getDistanceMiles() + delimiter + farm.getLastUpdated();
    }

    public int getSkippedLines() { return skippedLines; }
    public String getLastError() { return lastError; }
}