/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 7 - DMS Project Phase 1
 * Farm Finder
 */

import java.time.LocalDate;

/** information storing */
public class Farm {
    private final int farmId;
    private String name;
    private String city;
    private String products;
    private String season;
    private boolean isOrganic;
    private double distanceMiles;
    private LocalDate lastUpdated;

    public Farm(int farmId, String name, String city, String products,
                String season, boolean isOrganic, double distanceMiles,
                LocalDate lastUpdated) {
        this.farmId = farmId;
        this.name = name;
        this.city = city;
        this.products = products;
        this.season = season;
        this.isOrganic = isOrganic;
        this.distanceMiles = distanceMiles;
        this.lastUpdated = lastUpdated;
    }

    public int getFarmId() { return farmId; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getProducts() { return products; }
    public String getSeason() { return season; }
    public boolean isOrganic() { return isOrganic; }
    public double getDistanceMiles() { return distanceMiles; }
    public LocalDate getLastUpdated() { return lastUpdated; }

    public boolean setName(String name) { this.name = name; return true; }
    public boolean setCity(String city) { this.city = city; return true; }
    public boolean setProducts(String products) { this.products = products; return true; }
    public boolean setSeason(String season) { this.season = season; return true; }
    public boolean setOrganic(boolean organic) { this.isOrganic = organic; return true; }
    public boolean setDistanceMiles(double miles) { this.distanceMiles = miles; return true; }
    public boolean setLastUpdated(LocalDate date) { this.lastUpdated = date; return true; }

    /** separating products with a semicolon. */
    public boolean sellsProduct(String wantedProduct) {
        if (wantedProduct == null || wantedProduct.trim().isEmpty() || products == null) {
            return false;
        }
        for (String product : products.split(";")) {
            if (product.trim().equalsIgnoreCase(wantedProduct.trim())) {
                return true;
            }
        }
        return false;
    }

    /** makes one farm readable */
    @Override
    public String toString() {
        return "ID: " + farmId + " | Name: " + name + " | City: " + city
                + " | Products: " + products + " | Season: " + season
                + " | Organic: " + (isOrganic ? "Y" : "N")
                + " | Miles: " + distanceMiles + " | Updated: " + lastUpdated;
    }
}
