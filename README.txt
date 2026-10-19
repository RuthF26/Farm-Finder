FARM FINDER - MODULE 7: PHASE 1
Ruth Forzani
CEN 3024C - Software Development I

PROJECT DESCRIPTION
Farm Finder is a Java console program for managing a list of local farms.
Users can add, view, update, and remove farm records. The Find Local Organic
feature searches for organic farms by product and distance and shows the
closest matches first.

HOW TO RUN
1. Open the Farm Finder project folder in IntelliJ IDEA.
2. Make sure Java 17 or newer is selected.
3. Open src/FarmApp.java and run the main() method.
4. Choose an option from the menu.

MENU OPTIONS
1 - Add a farm
2 - View all farms
3 - Update a farm
4 - Remove a farm
5 - Find Local Organic
6 - Load farms from a text file
7 - Save and Exit

DATA FILES
farms.txt contains 20 sample farm records.
To load a file, choose option 6 and enter its name (for example, farms.txt).
The text file contain sample data for this school project.

NOTES
The program checks user input and displays messages for invalid entries.
Data is stored in memory while the program runs. Save and Exit writes the
records to the selected text file, replacing that file's previous contents.


To run the executable JAR from the project folder:
java -jar "Farm Finder.jar"
