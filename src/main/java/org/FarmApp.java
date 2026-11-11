/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 8 - DMS Project Phase 2
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
        this(new FarmManager(new FarmFileRepository(",")), new Scanner(System.in), "farms.txt");
    }

    // For testing with sample input and a separate file.
    FarmApp(FarmManager manager, Scanner input, String filePath) {
        this.manager = manager;
        this.validator = new FarmValidator();
        this.input = input;
        this.filePath = filePath;
    }

    public static void main(String[] args) {
        new FarmApp().run();
    }

    /** the menu repeats, even after invalid input, until option 7 succeeds. */
    public int run() {
        System.out.println("Welcome to Farm Finder - Osceola County!");
        try {
            while (true) {
                System.out.println(getMenuText());
                try {
                    int choice = readOptionInRange("Choose an option: ", 1, 7);
                    if (!handleChoice(choice)) {
                        return 0;
                    }
                } catch (BackToMenu exception) {
                    System.out.println("Returning to main menu. Completed updates are kept.");
                }
            }
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
                + "5. Find Local Farms\n"
                + "6. Load farms from file\n"
                + "7. Save & Exit\n"
                + "Type B at any prompt to return to this menu.";
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
            case 5 -> System.out.println(promptFindLocalFarms());
            case 6 -> System.out.println(promptLoadFile());
            case 7 -> {
                String message = saveAndExit();
                System.out.println(message);
                return !message.startsWith("Saved ");
            }
            default -> System.out.println("Please choose 1-7.");
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
        if (manager.findFarmById(id) == null) {
            return "No farm with ID " + id + ".";
        }
        while (true) {
            System.out.println("Current data: " + manager.findFarmById(id));
            System.out.println("1. Name  2. City  3. Products  4. Season"
                    + "  5. Organic  6. Miles  7. Farm ID  8. Last Updated  9. Done");
            int attribute = readOptionInRange("Choose a field (1-9): ", 1, 9);
            if (attribute == 9) {
                return "Finished updating farm " + id + ".";
            }
            String value = switch (attribute) {
                case 1 -> readText("New name: ");
                case 2 -> readText("New city: ");
                case 3 -> readProducts("New products: ");
                case 4 -> readSeason("New season: ");
                case 5 -> readYesNo("Organic? (Y/N): ") ? "Y" : "N";
                case 6 -> Double.toString(readDouble("New miles (0-200): "));
                case 7 -> Integer.toString(readNewId(id));
                case 8 -> readDate("New date (YYYY-MM-DD): ").toString();
                default -> "";
            };
            if (manager.updateFarm(id, attribute, value)) {
                if (attribute == 7) {
                    id = Integer.parseInt(value);
                }
                System.out.println("Farm updated. Choose another field or 9 to finish.");
            } else {
                System.out.println("Update could not be completed.");
            }
        }
    }

    private int readNewId(int currentId) {
        while (true) {
            int id = readInt("New farm ID: ");
            if (id == currentId || manager.findFarmById(id) == null) {
                return id;
            }
            System.out.println("ID " + id + " already exists. Try another ID.");
        }
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
    private String promptFindLocalFarms() {
        System.out.println("\n--- Find Local Farms ---");
        String product = readText("Product to find: ");
        double miles = readDouble("Maximum miles (0-200): ");
        boolean organicOnly = readYesNo("Organic farms only? (Y/N): ");
        ArrayList<Farm> matches = manager.findLocalFarms(product, miles, organicOnly);
        if (matches.isEmpty()) {
            if (organicOnly && !manager.findLocalFarms(product, miles, false).isEmpty()) {
                return "This product is available, but not at an organic farm within "
                        + formatMiles(miles) + " miles. Try again with organic = N.";
            }
            return "No " + (organicOnly ? "organic " : "") + "farms selling "
                    + product + " within " + formatMiles(miles) + " miles.";
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
        if (manager.getFarmCount() == 0
                && !readYesNo("No farms are loaded. Save an empty file? (Y/N): ")) {
            return "Save cancelled. Load or add farms first.";
        }
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
                // try again.
            }
            System.out.println("Please choose " + min + "-" + max + ".");
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
                // try again.
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
                // try again.
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
            System.out.println("Use at least one product and do not leave empty items between ;.");
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

    /** B returns to the main menu */
    private String readRawLine(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            throw new NoSuchElementException("Input stream closed");
        }
        String text = input.nextLine().trim();
        if (text.equalsIgnoreCase("B")) {
            throw new BackToMenu();
        }
        return text;
    }

    /** miles formating. */
    private String formatMiles(double miles) {
        if (miles == Math.rint(miles)) {
            return Long.toString((long) miles);
        }
        return Double.toString(miles);
    }

    private static class BackToMenu extends RuntimeException { }
}