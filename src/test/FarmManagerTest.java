/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 8 - DMS Project Phase 2
 * Farm Finder unit tests
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class FarmManagerTest {
    private FarmManager manager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        manager = new FarmManager(new FarmFileRepository(","));
    }

    private Farm farm(int id, String products, boolean organic, double miles) {
        return new Farm(id, "Test Farm " + id, "St. Cloud", products,
                "Year-round", organic, miles, LocalDate.now().minusDays(1));
    }

    @Test
    void addFarmStoresNewFarm() {
        assertTrue(manager.addFarm(farm(1, "eggs", true, 5)));
        assertEquals(1, manager.getFarmCount());
        assertEquals("eggs", manager.findFarmById(1).getProducts());
    }

    @Test
    void cannotAddDuplicateOrInvalidFarm() {
        assertTrue(manager.addFarm(farm(1, "eggs", true, 5)));
        assertFalse(manager.addFarm(farm(1, "milk", false, 10)));
        assertFalse(manager.addFarm(farm(-5, "eggs", true, 5)));
        assertFalse(manager.addFarm(null));
        assertEquals(1, manager.getFarmCount());
    }

    @Test
    void removeFarmDeletesExistingRecord() {
        manager.addFarm(farm(1, "eggs", true, 5));
        assertTrue(manager.removeFarm(1));
        assertNull(manager.findFarmById(1));
        assertEquals(0, manager.getFarmCount());
    }

    @Test
    void removeUnknownFarmDoesNotChangeData() {
        manager.addFarm(farm(1, "eggs", true, 5));
        assertFalse(manager.removeFarm(99));
        assertEquals(1, manager.getFarmCount());
    }

    @Test
    void updateAllTextFields() {
        manager.addFarm(farm(1, "eggs", true, 5));
        assertTrue(manager.updateFarm(1, 1, "New Name"));
        assertTrue(manager.updateFarm(1, 2, "Kissimmee"));
        assertTrue(manager.updateFarm(1, 3, "milk;cheese"));
        assertTrue(manager.updateFarm(1, 4, "Oct-May"));
        Farm updated = manager.findFarmById(1);
        assertEquals("New Name", updated.getName());
        assertEquals("Kissimmee", updated.getCity());
        assertEquals("milk;cheese", updated.getProducts());
        assertEquals("Oct-May", updated.getSeason());
    }

    @Test
    void updateOrganicDistanceIdAndDate() {
        manager.addFarm(farm(1, "milk", false, 11.7));
        assertTrue(manager.updateFarm(1, 5, "Y"));
        assertTrue(manager.updateFarm(1, 6, "8.25"));
        assertTrue(manager.updateFarm(1, 7, "200"));
        assertNull(manager.findFarmById(1));
        assertTrue(manager.updateFarm(200, 8, "2025-05-10"));
        Farm changed = manager.findFarmById(200);
        assertTrue(changed.isOrganic());
        assertEquals(8.25, changed.getDistanceMiles(), 0.001);
        assertEquals(LocalDate.of(2025, 5, 10), changed.getLastUpdated());
    }

    @Test
    void invalidUpdatesDoNotChangeTheFarm() {
        manager.addFarm(farm(1, "eggs", true, 5));
        manager.addFarm(farm(2, "milk", false, 9));
        assertFalse(manager.updateFarm(1, 1, ""));
        assertFalse(manager.updateFarm(1, 3, "eggs;;milk"));
        assertFalse(manager.updateFarm(1, 5, "maybe"));
        assertFalse(manager.updateFarm(1, 6, "NaN"));
        assertFalse(manager.updateFarm(1, 6, "-1"));
        assertFalse(manager.updateFarm(1, 7, "2"));
        assertFalse(manager.updateFarm(1, 7, "not a number"));
        assertFalse(manager.updateFarm(1, 8, "2999-01-01"));
        assertFalse(manager.updateFarm(1, 8, "yesterday"));
        assertFalse(manager.updateFarm(99, 1, "Name"));
        assertFalse(manager.updateFarm(1, 9, "Name"));
        Farm saved = manager.findFarmById(1);
        assertEquals("Test Farm 1", saved.getName());
        assertEquals("eggs", saved.getProducts());
        assertEquals(5, saved.getDistanceMiles(), 0.001);
        assertEquals(LocalDate.now().minusDays(1), saved.getLastUpdated());
        assertEquals(2, manager.getFarmCount());
    }

    @Test
    void milkFoundWhenOrganicFilterIsOff() {
        manager.addFarm(farm(1028, "milk;cheese;yogurt", false, 11.7));
        assertEquals(0, manager.findLocalFarms("milk", 20, true).size());
        assertEquals(1, manager.findLocalFarms("milk", 20, false).size());
        assertEquals(1028, manager.findLocalFarms("milk", 20, false).get(0).getFarmId());
        assertEquals(0, manager.findLocalOrganic("milk", 20).size());
    }

    @Test
    void organicSearchFindsOnlyOrganicFarms() {
        manager.addFarm(farm(1, "eggs", true, 7));
        manager.addFarm(farm(2, "eggs", false, 4));
        assertEquals(1, manager.findLocalOrganic("eggs", 10).size());
        assertEquals(1, manager.findLocalOrganic("eggs", 10).get(0).getFarmId());
    }

    @Test
    void searchSortsFarmsAndCalculatesAverage() {
        manager.addFarm(farm(1, "eggs", true, 10));
        manager.addFarm(farm(2, "eggs", true, 4));
        manager.addFarm(farm(3, "eggs", true, 30));
        ArrayList<Farm> result = manager.findLocalFarms(" EGGS ", 20, true);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getFarmId());
        assertEquals(1, result.get(1).getFarmId());
        assertEquals(7, manager.calculateAverageDistance(result), 0.001);
        assertEquals(0, manager.calculateAverageDistance(new ArrayList<>()), 0.001);
    }

    @Test
    void searchRejectsBadDistanceAndMissingProducts() {
        manager.addFarm(farm(1, "eggs", true, 5));
        assertTrue(manager.findLocalFarms("milk", 20, false).isEmpty());
        assertTrue(manager.findLocalFarms("eggs", -1, false).isEmpty());
        assertTrue(manager.findLocalFarms("eggs", Double.NaN, false).isEmpty());
        assertTrue(manager.findLocalFarms(" ", 20, false).isEmpty());
    }

    @Test
    void dataCanBeSavedAndLoaded() {
        Path file = tempDir.resolve("test-farms.txt");
        manager.addFarm(farm(1, "eggs", true, 5));
        assertTrue(manager.saveToFile(file.toString()));
        assertTrue(Files.exists(file));
        FarmManager newManager = new FarmManager(new FarmFileRepository(","));
        assertEquals(1, newManager.loadFromFile(file.toString()));
        assertEquals("eggs", newManager.findFarmById(1).getProducts());
    }

    @Test
    void failedFileLoadKeepsOldFarms() {
        manager.addFarm(farm(1, "eggs", true, 5));
        assertEquals(-1, manager.loadFromFile(tempDir.resolve("missing.txt").toString()));
        assertEquals(1, manager.getFarmCount());
        assertNotNull(manager.findFarmById(1));
        assertFalse(manager.getLastFileError().isEmpty());
    }

    @Test
    void loadingDuplicateIdsOnlyKeepsFirstRecord() throws Exception {
        Path file = tempDir.resolve("duplicate.txt");
        String record = "1,First Farm,St. Cloud,eggs,Year-round,true,5,2026-09-01\n";
        String duplicate = "1,Second Farm,St. Cloud,milk,Year-round,false,7,2026-09-01\n";
        Files.writeString(file, record + duplicate);
        assertEquals(1, manager.loadFromFile(file.toString()));
        assertEquals(1, manager.getLastSkippedLines());
        assertEquals("First Farm", manager.findFarmById(1).getName());
    }

    @Test
    void displayShowsDataOrEmptyMessage() {
        assertEquals("No farms to display.", manager.formatTable(manager.getAllFarms()));
        manager.addFarm(farm(1, "eggs", true, 5));
        assertTrue(manager.formatTable(manager.getAllFarms()).contains("Test Farm 1"));
    }
}