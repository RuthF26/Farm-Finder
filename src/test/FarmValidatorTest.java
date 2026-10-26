/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 8 - DMS Project Phase 2
 * Farm Finder unit tests
 */

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class FarmValidatorTest {
    private final FarmValidator validator = new FarmValidator();

    private Farm farm() {
        return new Farm(1, "Sunny Acres", "St. Cloud", "eggs;honey",
                "Oct-May", true, 4.2, LocalDate.now().minusDays(1));
    }

    @Test
    void validFarmPassesAllChecks() {
        assertTrue(validator.isValidFarm(farm()));
        assertEquals("", validator.getErrorMessage(farm()));
    }

    @Test
    void invalidFarmIdNameAndCityAreRejected() {
        Farm record = farm();
        assertTrue(record.setName(" "));
        assertFalse(validator.isValidFarm(record));
        record.setName("Sunny Acres");
        record.setCity("");
        assertFalse(validator.isValidFarm(record));
        record.setCity("St. Cloud");
        assertFalse(validator.isValidId(0));
        assertFalse(validator.isValidId(-100));
    }

    @Test
    void productListMustNotHaveEmptyItems() {
        assertTrue(validator.hasProduct("milk;cheese"));
        assertFalse(validator.hasProduct(";"));
        assertFalse(validator.hasProduct("milk;;cheese"));
        assertFalse(validator.hasProduct("milk;"));
        assertFalse(validator.hasProduct("  "));
        assertFalse(validator.hasProduct(null));
    }

    @Test
    void invalidDistanceAndFutureDatesAreRejected() {
        assertFalse(validator.isValidDistance(-0.1));
        assertFalse(validator.isValidDistance(201));
        assertFalse(validator.isValidDistance(Double.NaN));
        assertFalse(validator.isValidDistance(Double.POSITIVE_INFINITY));
        assertTrue(validator.isValidDistance(0));
        assertTrue(validator.isValidDistance(200));
        assertFalse(validator.isValidDate(LocalDate.now().plusDays(1)));
        assertFalse(validator.isValidDate(null));
    }

    @Test
    void commasAndNewlinesAreRejected() {
        assertFalse(validator.isSafeFileText("Hello, world"));
        assertFalse(validator.isSafeFileText("Hello\nworld"));
        Farm record = farm();
        record.setName("Sunny, Acres");
        assertFalse(validator.isValidFarm(record));
    }
}