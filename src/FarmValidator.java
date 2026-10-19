/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 7 - DMS Project Phase 1
 * Farm Finder
 */

import java.time.LocalDate;

/** validation rules */
public class FarmValidator {
    private final double maxMiles = 200.0;

    public FarmValidator() { }

    public boolean isValidId(int farmId) {
        return farmId > 0;
    }

    public boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public boolean isValidDistance(double miles) {
        return Double.isFinite(miles) && miles >= 0.0 && miles <= maxMiles;
    }

    public boolean isValidDate(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    public boolean isSafeFileText(String text) {
        return text != null && !text.contains(",") && !text.contains("\n")
                && !text.contains("\r");
    }

    /** products must contain atleast 1 item and a semicolon separating them */
    public boolean hasProduct(String products) {
        if (!isNotBlank(products)) {
            return false;
        }
        for (String item : products.split(";")) {
            if (isNotBlank(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidFarm(Farm farm) {
        return getErrorMessage(farm).isEmpty();
    }

    /** return empty if invalid data */
    public String getErrorMessage(Farm farm) {
        if (farm == null) {
            return "Farm record is missing.";
        }
        if (!isValidId(farm.getFarmId())) {
            return "Enter a whole number above 0 for the farm ID.";
        }
        if (!isNotBlank(farm.getName()) || !isNotBlank(farm.getCity())
                || !hasProduct(farm.getProducts())) {
            return "Name, city, and products cannot be blank.";
        }
        if (!isSafeFileText(farm.getName()) || !isSafeFileText(farm.getCity())
                || !isSafeFileText(farm.getProducts()) || !isSafeFileText(farm.getSeason())) {
            return "Text fields cannot contain commas or line breaks.";
        }
        if (!isValidDistance(farm.getDistanceMiles())) {
            return "Enter a number from 0 to 200 for the distance.";
        }
        if (!isValidDate(farm.getLastUpdated())) {
            return "Use YYYY-MM-DD; date can't be in the future.";
        }
        return "";
    }
}
