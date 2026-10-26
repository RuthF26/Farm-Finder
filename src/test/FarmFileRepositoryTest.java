/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 8 - DMS Project Phase 2
 * Farm Finder unit tests
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class FarmFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void opensAndReadsAValidTextFile() throws Exception {
        Path path = tempDir.resolve("farms.txt");
        Files.writeString(path, "10,Test Farm,St. Cloud,milk;cheese,Year-round,false,11.7,2026-09-26\n");
        FarmFileRepository repository = new FarmFileRepository(",");
        ArrayList<Farm> farms = repository.loadFarms(path.toString());
        assertEquals(1, farms.size());
        assertEquals(10, farms.get(0).getFarmId());
        assertEquals("milk;cheese", farms.get(0).getProducts());
        assertEquals(0, repository.getSkippedLines());
    }

    @Test
    void skipsBadLinesAndDoesNotTreatInvalidBooleanAsFalse() throws Exception {
        Path path = tempDir.resolve("mixed.txt");
        Files.writeString(path,
                "10,Test Farm,St. Cloud,eggs,Year-round,true,5,2026-09-01\n"
                        + "11,Bad Farm,St. Cloud,milk,Year-round,maybe,4,2026-09-01\n"
                        + "12,Bad Farm,St. Cloud,eggs;;milk,Year-round,true,4,2026-09-01\n"
                        + "\n");
        FarmFileRepository repository = new FarmFileRepository(",");
        assertEquals(1, repository.loadFarms(path.toString()).size());
        assertEquals(3, repository.getSkippedLines());
    }

    @Test
    void savingAndReadingKeepsAllFields() {
        Farm farm = new Farm(10, "Test Farm", "St. Cloud", "milk;cheese",
                "Year-round", false, 11.7, LocalDate.now().minusDays(2));
        FarmFileRepository repository = new FarmFileRepository(",");
        ArrayList<Farm> farms = new ArrayList<>();
        farms.add(farm);
        Path file = tempDir.resolve("saved.txt");
        assertTrue(repository.saveFarms(file.toString(), farms));
        Farm readBack = repository.loadFarms(file.toString()).get(0);
        assertEquals(farm.getName(), readBack.getName());
        assertEquals(farm.getProducts(), readBack.getProducts());
        assertEquals(farm.isOrganic(), readBack.isOrganic());
        assertEquals(farm.getDistanceMiles(), readBack.getDistanceMiles(), 0.001);
        assertEquals(farm.getLastUpdated(), readBack.getLastUpdated());
    }

    @Test
    void blankAndMissingFileNamesFailSafely() {
        FarmFileRepository repository = new FarmFileRepository(",");
        assertTrue(repository.loadFarms("  ").isEmpty());
        assertFalse(repository.getLastError().isEmpty());
        assertTrue(repository.loadFarms(tempDir.resolve("missing.txt").toString()).isEmpty());
        assertFalse(repository.getLastError().isEmpty());
        assertFalse(repository.saveFarms(" ", new ArrayList<>()));
    }

    @Test
    void invalidFarmCannotBeSaved() {
        FarmFileRepository repository = new FarmFileRepository(",");
        ArrayList<Farm> farms = new ArrayList<>();
        farms.add(new Farm(1, "", "St. Cloud", "eggs", "Oct-May",
                true, 5, LocalDate.now()));
        assertFalse(repository.saveFarms(tempDir.resolve("bad.txt").toString(), farms));
    }
}