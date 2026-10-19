/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 7 - DMS Project Phase 1: Logic and Input Validation
 * Farm Finder
 */

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** displays menu and results open until Save & Exit. */
public class FarmApp {
    private final FarmManager manager;
    private final FarmValidator validator;
    private final Scanner input;
    private String filePath;

    public FarmApp() {
        this.manager = new FarmManager(new FarmFileRepository(","));
        this.validator = new FarmValidator();
        this.input = new Scanner(System.in);
        this.filePath = "farms.txt";
    }


    public static void main(String[] args) {
        new FarmApp().run();
    }

    /** the menu repeats, even after invalid input, until option 7 succeeds. */
    public int run() {
        System.out.println("Welcome to Farm Finder - Osceola County!");
        try {
            boolean keepRunning = true;
            while (keepRunning) {
                System.out.println(getMenuText());
                int choice = readOptionInRange("Choose an option: ", 1, 7);
                keepRunning = handleChoice(choice);
            }
            return 0;
        } catch (NoSuchElementException exception) {
            System.out.println("Input was closed. Program stopped without saving.");
            return 1;
        }
    }

    private String getMenuText() {
        return "\n===== FARM FINDER =====\n"
                + "1. Add a farm\n"
                + "2. View all farms\n"
                + "3. Update a farm\n"
                + "4. Remove a farm\n"
                + "5. Find Local Organic\n"
                + "6. Load farms from file\n"
                + "7. Save & Exit";
    }

    /** Here is the menu switch case */
    private boolean handleChoice(int choice) {
        switch (choice) {
            case 1 -> {
                Farm farm = promptNewFarm();
                if (manager.addFarm(farm)) {
                    System.out.println("Farm " + farm.getFarmId() + " added.");
                    System.out.println(farm);
                } else {
                    System.out.println("Farm was not added. Check the information.");
                }
            }
            case 2 -> System.out.println(manager.formatTable(manager.getAllFarms()));
            case 3 -> System.out.println(promptUpdate());
            case 4 -> System.out.println(promptRemove());
            case 5 -> System.out.println(promptFindLocalOrganic());
            case 6 -> System.out.println(promptLoadFile());
            case 7 -> {
                String message = saveAndExit();
                System.out.println(message);
                return !message.startsWith("Saved ");
            }
            default -> System.out.println("Please choose 1–7.");
        }
        return true;
    }

    /** field collection and id checking */
    private Farm promptNewFarm() {
        System.out.println("\n--- Add a farm ---");
        int id;
        while (true) {
            id = readInt("Farm ID: ");
            if (manager.findFarmById(id) == null) {
                break;
            }
            System.out.println("ID " + id + " already exists. Try another ID.");
        }
        String name = readText("Farm name: ");
        String city = readText("City: ");
        String products = readProducts("Products (separate with ;): ");
        String season = readSeason("Season (e.g. Oct-May; may be blank): ");
        boolean organic = readYesNo("Organic? (Y/N): ");
        double miles = readDouble("Distance in miles (0-200): ");
        LocalDate date = readDate("Last updated (YYYY-MM-DD): ");
        return new Farm(id, name, city, products, season, organic, miles, date);
    }

    /** updatable attributes for the farm finder */
    private String promptUpdate() {
        System.out.println("\n--- Update a farm ---");
        int id = readInt("Farm ID to update: ");
        Farm farm = manager.findFarmById(id);
        if (farm == null) {
            return "No farm with ID " + id + ".";
        }
        System.out.println("Current data: " + farm);
        System.out.println("1. Name  2. City  3. Products  4. Season  5. Organic  6. Miles");
        int attribute = readOptionInRange("Choose a field (1-6): ", 1, 6);
        String value = switch (attribute) {
            case 1 -> readText("New name: ");
            case 2 -> readText("New city: ");
            case 3 -> readProducts("New products: ");
            case 4 -> readSeason("New season: ");
            case 5 -> readYesNo("Organic? (Y/N): ") ? "Y" : "N";
            case 6 -> Double.toString(readDouble("New miles (0-200): "));
            default -> "";
        };
        if (!manager.updateFarm(id, attribute, value)) {
            return "Update could not be completed.";
        }
        return "Farm " + id + " updated.\n" + manager.findFarmById(id);
    }

    private String promptRemove() {
        System.out.println("\n--- Remove a farm ---");
        int id = readInt("Farm ID to remove: ");
        Farm farm = manager.findFarmById(id);
        if (farm == null) {
            return "No farm with ID " + id + ".";
        }
        if (!readYesNo("Remove " + farm.getName() + "? (Y/N): ")) {
            return "Cancelled. Farm " + id + " was kept.";
        }
        return manager.removeFarm(id) ? "Farm " + id + " removed."
                : "Farm could not be removed.";
    }

    /** filtering, sorting and looking for the average distance a.k.a the custom action*/
    private String promptFindLocalOrganic() {
        System.out.println("\n--- Find Local Organic ---");
        String product = readText("Product to find: ");
        double miles = readDouble("Maximum miles (0-200): ");
        ArrayList<Farm> matches = manager.findLocalOrganic(product, miles);
        if (matches.isEmpty()) {
            return "No organic farms selling " + product + " within "
                    + formatMiles(miles) + " miles.";
        }
        double average = manager.calculateAverageDistance(matches);
        return manager.formatTable(matches) + "Matches: " + matches.size()
                + " | Average distance: "
                + String.format(Locale.US, "%.2f", average) + " miles.";
    }

    /** invalid file name can't be added */
    private String promptLoadFile() {
        System.out.println("\n--- Load farms from file ---");
        String path = readRawLine("Text file path (e.g. farms.txt): ");
        if (path.isEmpty()) {
            return "File name can't be blank. No data loaded.";
        }
        int loaded = manager.loadFromFile(path);
        if (loaded < 0) {
            return manager.getLastFileError() + " Previous data was kept.";
        }
        filePath = path;
        return loaded + " farm(s) loaded from " + path + ". Skipped lines: "
                + manager.getLastSkippedLines() + ".";
    }

    /** saves and exits */
    private String saveAndExit() {
        if (manager.saveToFile(filePath)) {
            return "Saved " + manager.getFarmCount() + " farm(s) to " + filePath
                    + ". Goodbye!";
        }
        return manager.getLastFileError()
                + " Save failed; your records are still in memory. Try again.";
    }

    private int readOptionInRange(String prompt, int min, int max) {
        while (true) {
            String text = readRawLine(prompt);
            try {
                int number = Integer.parseInt(text);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException exception) {
            }
            System.out.println("Please choose " + min + "–" + max + ".");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            String text = readRawLine(prompt);
            try {
                int number = Integer.parseInt(text);
                if (validator.isValidId(number)) {
                    return number;
                }
            } catch (NumberFormatException exception) {
            }
            System.out.println("Enter a whole number above 0.");
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            String text = readRawLine(prompt);
            try {
                double miles = Double.parseDouble(text);
                if (validator.isValidDistance(miles)) {
                    return miles;
                }
            } catch (NumberFormatException exception) {
            }
            System.out.println("Enter a number from 0 to 200.");
        }
    }

    private String readText(String prompt) {
        while (true) {
            String text = readRawLine(prompt);
            if (!validator.isNotBlank(text)) {
                System.out.println("This field can't be blank.");
            } else if (!validator.isSafeFileText(text)) {
                System.out.println("Please do not use commas in a text field.");
            } else {
                return text;
            }
        }
    }

    private String readProducts(String prompt) {
        while (true) {
            String products = readText(prompt);
            if (validator.hasProduct(products)) {
                return products;
            }
            System.out.println("Please enter at least one product, not only semicolons.");
        }
    }

    private String readSeason(String prompt) {
        while (true) {
            String season = readRawLine(prompt);
            if (validator.isSafeFileText(season)) {
                return season;
            }
            System.out.println("Please do not use commas in a text field.");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            String response = readRawLine(prompt);
            if (response.equalsIgnoreCase("Y")) {
                return true;
            }
            if (response.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("Please type Y or N.");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String text = readRawLine(prompt);
            try {
                LocalDate date = LocalDate.parse(text);
                if (validator.isValidDate(date)) {
                    return date;
                }
            } catch (DateTimeParseException exception) {
                // incorrect format or date in the future
            }
            System.out.println("Use YYYY-MM-DD; date can't be in the future.");
        }
    }

    private String readRawLine(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            throw new NoSuchElementException("Input stream closed");
        }
        return input.nextLine().trim();
    }

    /** miles formating. */
    private String formatMiles(double miles) {
        if (miles == Math.rint(miles)) {
            return Long.toString((long) miles);
        }
        return Double.toString(miles);
    }
}
