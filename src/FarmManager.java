/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 7 - DMS Project Phase 1
 * Farm Finder: CRUD and Find Local Organic search.
 */

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FarmManager {
    private final ArrayList<Farm> farms;
    private final FarmValidator validator;
    private final FarmFileRepository repository;
    private int lastSkippedLines;
    private String lastFileError;

    public FarmManager(FarmFileRepository repository) {
        this.farms = new ArrayList<>();
        this.validator = new FarmValidator();
        this.repository = repository;
        this.lastSkippedLines = 0;
        this.lastFileError = "";
    }

    public boolean addFarm(Farm farm) {
        if (!validator.isValidFarm(farm) || isDuplicateId(farm.getFarmId())) {
            return false;
        }
        return farms.add(farm);
    }

    public boolean removeFarm(int farmId) {
        for (int index = 0; index < farms.size(); index++) {
            if (farms.get(index).getFarmId() == farmId) {
                farms.remove(index);
                return true;
            }
        }
        return false;
    }

    /** farm updates when one field is changed */
    public boolean updateFarm(int farmId, int attribute, String newValue) {
        Farm oldFarm = findFarmById(farmId);
        if (oldFarm == null || attribute < 1 || attribute > 6 || newValue == null) {
            return false;
        }
        String value = newValue.trim();
        Farm edited = new Farm(oldFarm.getFarmId(), oldFarm.getName(),
                oldFarm.getCity(), oldFarm.getProducts(), oldFarm.getSeason(),
                oldFarm.isOrganic(), oldFarm.getDistanceMiles(), LocalDate.now());
        switch (attribute) {
            case 1 -> edited.setName(value);
            case 2 -> edited.setCity(value);
            case 3 -> edited.setProducts(value);
            case 4 -> edited.setSeason(value);
            case 5 -> {
                if (!value.equalsIgnoreCase("Y") && !value.equalsIgnoreCase("N")) {
                    return false;
                }
                edited.setOrganic(value.equalsIgnoreCase("Y"));
            }
            case 6 -> {
                try {
                    double miles = Double.parseDouble(value);
                    if (!validator.isValidDistance(miles)) {
                        return false;
                    }
                    edited.setDistanceMiles(miles);
                } catch (NumberFormatException exception) {
                    return false;
                }
            }
            default -> { return false; }
        }
        if (!validator.isValidFarm(edited)) {
            return false;
        }
        for (int index = 0; index < farms.size(); index++) {
            if (farms.get(index).getFarmId() == farmId) {
                farms.set(index, edited);
                return true;
            }
        }
        return false;
    }

    public Farm findFarmById(int farmId) {
        for (Farm farm : farms) {
            if (farm.getFarmId() == farmId) {
                return farm;
            }
        }
        return null;
    }

    public ArrayList<Farm> getAllFarms() {
        return new ArrayList<>(farms);
    }

    public int getFarmCount() { return farms.size(); }

    /** successful load replacing the old list */
    public int loadFromFile(String path) {
        ArrayList<Farm> loaded = repository.loadFarms(path);
        lastFileError = repository.getLastError();
        lastSkippedLines = repository.getSkippedLines();
        if (!lastFileError.isEmpty()) {
            return -1;
        }
        ArrayList<Farm> unique = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (Farm farm : loaded) {
            if (ids.add(farm.getFarmId())) {
                unique.add(farm);
            } else {
                lastSkippedLines++;
            }
        }
        farms.clear();
        farms.addAll(unique);
        return farms.size();
    }

    public boolean saveToFile(String path) {
        boolean saved = repository.saveFarms(path, farms);
        lastFileError = repository.getLastError();
        return saved;
    }

    /** custom action handling */
    public ArrayList<Farm> findLocalOrganic(String product, double maxMiles) {
        ArrayList<Farm> matches = new ArrayList<>();
        if (!validator.isNotBlank(product) || !validator.isValidDistance(maxMiles)) {
            return matches;
        }
        for (Farm farm : farms) {
            if (farm.isOrganic() && farm.sellsProduct(product)
                    && farm.getDistanceMiles() <= maxMiles) {
                matches.add(farm);
            }
        }
        matches.sort(Comparator.comparingDouble(Farm::getDistanceMiles));
        return matches;
    }

    public double calculateAverageDistance(ArrayList<Farm> matches) {
        if (matches == null || matches.isEmpty()) {
            return 0.0;
        }
        double totalMiles = 0.0;
        for (Farm farm : matches) {
            totalMiles += farm.getDistanceMiles();
        }
        return totalMiles / matches.size();
    }

    /** display all fields */
    public String formatTable(ArrayList<Farm> list) {
        if (list == null || list.isEmpty()) {
            return "No farms to display.";
        }
        StringBuilder output = new StringBuilder();
        output.append(String.format(Locale.US,
                "%-6s %-24s %-16s %-26s %-15s %-8s %-8s %s%n",
                "ID", "Name", "City", "Products", "Season", "Organic", "Miles", "Updated"));
        output.append("-".repeat(132)).append(System.lineSeparator());
        for (Farm farm : list) {
            output.append(String.format(Locale.US,
                    "%-6d %-24s %-16s %-26s %-15s %-8s %-8.1f %s%n",
                    farm.getFarmId(), farm.getName(), farm.getCity(),
                    farm.getProducts(), farm.getSeason(),
                    farm.isOrganic() ? "Y" : "N", farm.getDistanceMiles(),
                    farm.getLastUpdated()));
        }
        return output.toString();
    }

    public int getLastSkippedLines() { return lastSkippedLines; }
    public String getLastFileError() { return lastFileError; }

    private boolean isDuplicateId(int farmId) {
        return findFarmById(farmId) != null;
    }
}
