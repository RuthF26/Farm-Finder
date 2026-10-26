/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * Module 8 - DMS Project Phase 2
 * Farm Finder menu tests
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

public class FarmAppTest {
    @TempDir
    Path tempDir;

    private FarmManager newManager() {
        return new FarmManager(new FarmFileRepository(","));
    }

    private Farm sampleFarm() {
        return new Farm(101, "Test Farm", "St. Cloud", "milk;cheese",
                "Year-round", false, 11.7, LocalDate.now().minusDays(1));
    }

    // runs the real menu with typed input instead of using the keyboard.
    private String runCommands(FarmManager manager, String commands) {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream captured = new PrintStream(buffer, true, StandardCharsets.UTF_8);
             Scanner scripted = new Scanner(new StringReader(commands))) {
            System.setOut(captured);
            FarmApp app = new FarmApp(manager, scripted,
                    tempDir.resolve("test-output.txt").toString());
            assertEquals(0, app.run());
        } finally {
            System.setOut(oldOut);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void cancelAddDoesNotCreateHalfFinishedFarm() {
        FarmManager manager = newManager();
        String output = runCommands(manager, "1\n200\nUnfinished Farm\nB\n7\nY\n");
        assertEquals(0, manager.getFarmCount());
        assertTrue(output.contains("Returning to main menu"));
    }

    @Test
    void invalidMenuChoicesDoNotStopProgram() {
        FarmManager manager = newManager();
        manager.addFarm(sampleFarm());
        String output = runCommands(manager, "xyz\n99\n2\n7\n");
        assertTrue(output.contains("Please choose 1-7"));
        assertTrue(output.contains("Test Farm"));
        assertTrue(output.contains("Saved 1 farm(s)"));
    }

    @Test
    void multipleUpdatesCanBeMadeInOneVisit() {
        FarmManager manager = newManager();
        manager.addFarm(sampleFarm());
        String output = runCommands(manager, "3\n101\n1\nUpdated Farm\n6\n8.5\n9\n7\n");
        assertEquals("Updated Farm", manager.findFarmById(101).getName());
        assertEquals(8.5, manager.findFarmById(101).getDistanceMiles(), 0.001);
        assertTrue(output.contains("Finished updating farm 101"));
    }

    @Test
    void changedFarmIdCanStillBeUpdated() {
        FarmManager manager = newManager();
        manager.addFarm(sampleFarm());
        runCommands(manager, "3\n101\n7\n202\n2\nNarcoossee\n9\n7\n");
        assertNull(manager.findFarmById(101));
        assertEquals("Narcoossee", manager.findFarmById(202).getCity());
    }

    @Test
    void milkShowsWhenOrganicOnlyIsNo() {
        FarmManager manager = newManager();
        manager.addFarm(sampleFarm());
        String output = runCommands(manager, "5\nmilk\n20\nY\n5\nmilk\n20\nN\n7\n");
        assertTrue(output.contains("not at an organic farm"));
        assertTrue(output.contains("Matches: 1 | Average distance: 11.70 miles"));
    }

    @Test
    void cancelRemoveKeepsTheFarm() {
        FarmManager manager = newManager();
        manager.addFarm(sampleFarm());
        runCommands(manager, "4\n101\nB\n7\n");
        assertNotNull(manager.findFarmById(101));
    }

    @Test
    void badInputInAddPromptsIsHandled() {
        FarmManager manager = newManager();
        String commands = "1\nwrong\n-1\n101\nExample Farm\nSt. Cloud\n"
                + "milk;;cheese\nmilk;cheese\n\nmaybe\nY\nNaN\n11.7\n"
                + "2999-01-01\n" + LocalDate.now().minusDays(1) + "\n7\n";
        String output = runCommands(manager, commands);
        assertEquals(1, manager.getFarmCount());
        assertTrue(output.contains("Enter a whole number above 0"));
        assertTrue(output.contains("do not leave empty items"));
        assertTrue(output.contains("Please type Y or N"));
        assertTrue(output.contains("Enter a number from 0 to 200"));
        assertTrue(output.contains("date can't be in the future"));
    }

    @Test
    void backWorksWhileEnteringASearch() {
        FarmManager manager = newManager();
        manager.addFarm(sampleFarm());
        String output = runCommands(manager, "5\nmilk\nB\n2\n7\n");
        assertTrue(output.contains("Returning to main menu"));
        assertTrue(output.contains("Test Farm"));
    }
}