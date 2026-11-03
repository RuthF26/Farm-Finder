/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * DMS Project - Phase 3 GUI
 * Farm Finder
 */

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** here we have the GUI for the farm finder. */
public class FarmGUI extends JFrame {
    private static final Color DARK = new Color(31, 61, 46);
    private static final Color GREEN = new Color(79, 127, 52);
    private static final Color ORANGE = new Color(192, 95, 55);
    private static final Color PALE = new Color(237, 243, 235);
    private static final Color TEXT = new Color(45, 57, 49);
    private static final Color BORDER = new Color(195, 210, 192);

    private final FarmManager manager = new FarmManager(new FarmFileRepository(","));
    private final FarmValidator validator = new FarmValidator();
    private String filePath = "farms.txt";
    private final ArrayList<Farm> displayedFarms = new ArrayList<>();

    private final JTextField productSearch = new JTextField(16);
    private final JTextField maxMilesSearch = new JTextField("15", 5);
    private final JCheckBox searchOrganic = new JCheckBox("Organic only", true);
    private JButton searchButton;

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField cityField = new JTextField();
    private final JTextField productsField = new JTextField();
    private final JTextField seasonField = new JTextField();
    private final JTextField distanceField = new JTextField();
    private final JTextField updatedField = new JTextField();
    private final JCheckBox organicField = new JCheckBox("Organic");
    private final JLabel status = new JLabel("Ready. Add a farm or load a text file.");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "City", "Mi", "Org"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public FarmGUI() {
        super("Farm Finder: Osceola County");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(990, 630));
        setSize(1110, 710);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        setContentPane(root);
        root.add(makeHeader(), BorderLayout.NORTH);
        root.add(makeMainPanel(), BorderLayout.CENTER);
        root.add(makeStatusBar(), BorderLayout.SOUTH);

        // select a farm in the table to add the information in the form.
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < displayedFarms.size()) {
                    showFarm(displayedFarms.get(row));
                }
            }
        });

        // pressing the x button with bring save and exit.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                saveAndExit();
            }
        });
    }

    private JPanel makeHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 21, 10, 21));

        JLabel title = new JLabel("Farm Finder: Osceola County");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        header.add(title, BorderLayout.WEST);

        JButton save = makeButton("Save & Exit", ORANGE);
        save.setPreferredSize(new Dimension(143, 34));
        save.addActionListener(event -> saveAndExit());
        header.add(save, BorderLayout.EAST);
        return header;
    }

    private JPanel makeMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));
        panel.add(makeSearchPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.WHITE);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.55;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 20);
        center.add(makeTablePanel(), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.45;
        constraints.insets = new Insets(0, 0, 0, 0);
        center.add(makeDetailsPanel(), constraints);
        panel.add(center, BorderLayout.CENTER);
        panel.add(makeActionsPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel makeSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(Color.WHITE);
        panel.add(makeLabel("Product"));
        styleTextField(productSearch);
        productSearch.setPreferredSize(new Dimension(175, 40));
        panel.add(productSearch);
        panel.add(makeLabel("Max miles"));
        styleTextField(maxMilesSearch);
        maxMilesSearch.setPreferredSize(new Dimension(80, 40));
        panel.add(maxMilesSearch);

        searchOrganic.setBackground(Color.WHITE);
        searchOrganic.setForeground(TEXT);
        searchOrganic.setFont(new Font("SansSerif", Font.PLAIN, 13));
        panel.add(searchOrganic);

        searchButton = makeButton("Find Local Organic", ORANGE);
        searchButton.setPreferredSize(new Dimension(182, 43));
        panel.add(searchButton);

        // Reset search results without deleting any loaded farms.
        JButton clearSearchButton = makeButton("Clear Search", ORANGE);
        clearSearchButton.setPreferredSize(new Dimension(150, 43));
        clearSearchButton.setToolTipText("Remove search filters and show all loaded farms.");
        clearSearchButton.addActionListener(event -> clearSearch());
        panel.add(clearSearchButton);

        searchOrganic.addActionListener(event -> searchButton.setText(
                searchOrganic.isSelected() ? "Find Local Organic" : "Find Local Farms"));
        searchButton.addActionListener(event -> findLocalFarms());
        productSearch.addActionListener(event -> findLocalFarms());
        maxMilesSearch.addActionListener(event -> findLocalFarms());
        return panel;
    }

    private JScrollPane makeTablePanel() {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setRowHeight(42);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(GREEN);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setPreferredSize(new Dimension(100, 40));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                                                           boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 5));
                setForeground(TEXT);
                setBackground(selected ? new Color(244, 226, 218)
                        : row % 2 == 0 ? Color.WHITE : PALE);
                return this;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(54);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(175);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(52);
        table.getColumnModel().getColumn(3).setMaxWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(45);
        table.getColumnModel().getColumn(4).setMaxWidth(65);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel makeDetailsPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        JLabel heading = new JLabel("FARM DETAILS");
        heading.setForeground(DARK);
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setBorder(BorderFactory.createEmptyBorder(7, 0, 11, 0));
        container.add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        addFormRow(form, 0, "ID", idField);
        addFormRow(form, 1, "Name", nameField);
        addFormRow(form, 2, "City", cityField);
        addFormRow(form, 3, "Products", productsField);
        addFormRow(form, 4, "Season", seasonField);
        addFormRow(form, 5, "Miles", distanceField);
        addFormRow(form, 6, "Updated", updatedField);

        organicField.setBackground(Color.WHITE);
        organicField.setForeground(TEXT);
        organicField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        GridBagConstraints box = new GridBagConstraints();
        box.gridx = 1;
        box.gridy = 7;
        box.anchor = GridBagConstraints.WEST;
        box.insets = new Insets(7, 0, 0, 0);
        form.add(organicField, box);

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = 8;
        spacer.weighty = 1;
        spacer.fill = GridBagConstraints.VERTICAL;
        form.add(Box.createVerticalGlue(), spacer);

        // keep the form controls at the top no matter what screen size.
        container.add(form, BorderLayout.CENTER);
        return container;
    }

    private void addFormRow(JPanel form, int row, String label, JTextField field) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(4, 0, 4, 15);
        form.add(makeLabel(label), left);

        styleTextField(field);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(4, 0, 4, 4);
        form.add(field, right);
    }

    private JPanel makeActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 18, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JButton add = makeButton("Add", GREEN);
        JButton update = makeButton("Update", GREEN);
        JButton delete = makeButton("Delete", ORANGE);
        JButton load = makeButton("Load File", GREEN);
        JButton clear = makeButton("Clear", GREEN);
        add.addActionListener(event -> addFarm());
        update.addActionListener(event -> updateFarm());
        delete.addActionListener(event -> deleteFarm());
        load.addActionListener(event -> loadFile());
        clear.addActionListener(event -> clearDetailsForm());
        for (JButton button : new JButton[]{add, update, delete, load, clear}) {
            button.setPreferredSize(new Dimension(140, 44));
            panel.add(button);
        }
        clear.setToolTipText("Clear farm detail fields to add a new farm. Search results stay visible.");
        return panel;
    }

    private JPanel makeStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PALE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(13, 21, 13, 21)));
        status.setFont(new Font("SansSerif", Font.BOLD, 13));
        status.setForeground(DARK);
        panel.add(status, BorderLayout.CENTER);
        return panel;
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    private JButton makeButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setPreferredSize(new Dimension(140, 38));
        Border outside = BorderFactory.createLineBorder(BORDER, 1);
        Border inside = BorderFactory.createEmptyBorder(6, 9, 6, 9);
        field.setBorder(BorderFactory.createCompoundBorder(outside, inside));
    }

    private void showFarm(Farm farm) {
        idField.setText(Integer.toString(farm.getFarmId()));
        nameField.setText(farm.getName());
        cityField.setText(farm.getCity());
        productsField.setText(farm.getProducts());
        seasonField.setText(farm.getSeason());
        distanceField.setText(Double.toString(farm.getDistanceMiles()));
        updatedField.setText(farm.getLastUpdated().toString());
        organicField.setSelected(farm.isOrganic());
    }

    private void clearForm() {
        for (JTextField field : new JTextField[]{idField, nameField, cityField,
                productsField, seasonField, distanceField, updatedField}) {
            field.setText("");
        }
        organicField.setSelected(false);
    }

    private void refreshTable(List<Farm> farms) {
        displayedFarms.clear();
        displayedFarms.addAll(farms);
        tableModel.setRowCount(0);
        for (Farm farm : displayedFarms) {
            tableModel.addRow(new Object[]{farm.getFarmId(), farm.getName(),
                    farm.getCity(), String.format(Locale.US, "%.1f", farm.getDistanceMiles()),
                    farm.isOrganic() ? "✓" : ""});
        }
        table.clearSelection();
        clearForm();
    }

    private void selectFarm(int id) {
        for (int row = 0; row < displayedFarms.size(); row++) {
            if (displayedFarms.get(row).getFarmId() == id) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                return;
            }
        }
    }

    private Farm selectedFarm() {
        int row = table.getSelectedRow();
        return row >= 0 && row < displayedFarms.size() ? displayedFarms.get(row) : null;
    }

    private Farm readForm() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            double miles = Double.parseDouble(distanceField.getText().trim());
            LocalDate date = LocalDate.parse(updatedField.getText().trim());
            Farm farm = new Farm(id, nameField.getText().trim(), cityField.getText().trim(),
                    productsField.getText().trim(), seasonField.getText().trim(),
                    organicField.isSelected(), miles, date);
            String error = validator.getErrorMessage(farm);
            if (!error.isEmpty()) {
                throw new IllegalArgumentException(error);
            }
            return farm;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID must be a positive whole number; miles must be 0 to 200.");
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Updated must be a valid date in YYYY-MM-DD format.");
        }
    }

    private void addFarm() {
        try {
            Farm farm = readForm();
            if (!manager.addFarm(farm)) {
                throw new IllegalArgumentException("That farm ID already exists.");
            }
            refreshTable(manager.getAllFarms());
            selectFarm(farm.getFarmId());
            setStatus("✓ " + farm.getName() + " added.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void updateFarm() {
        Farm old = selectedFarm();
        if (old == null) {
            showError("Select a farm in the table before updating.");
            return;
        }
        try {
            Farm edited = readForm();
            int oldId = old.getFarmId();
            int newId = edited.getFarmId();
            if (newId != oldId && manager.findFarmById(newId) != null) {
                throw new IllegalArgumentException("That farm ID already exists.");
            }

            // updating the changed fields
            boolean changed = false;
            if (!Objects.equals(old.getName(), edited.getName())) {
                manager.updateFarm(oldId, 1, edited.getName()); changed = true;
            }
            if (!Objects.equals(old.getCity(), edited.getCity())) {
                manager.updateFarm(oldId, 2, edited.getCity()); changed = true;
            }
            if (!Objects.equals(old.getProducts(), edited.getProducts())) {
                manager.updateFarm(oldId, 3, edited.getProducts()); changed = true;
            }
            if (!Objects.equals(old.getSeason(), edited.getSeason())) {
                manager.updateFarm(oldId, 4, edited.getSeason()); changed = true;
            }
            if (old.isOrganic() != edited.isOrganic()) {
                manager.updateFarm(oldId, 5, edited.isOrganic() ? "Y" : "N"); changed = true;
            }
            if (Double.compare(old.getDistanceMiles(), edited.getDistanceMiles()) != 0) {
                manager.updateFarm(oldId, 6, Double.toString(edited.getDistanceMiles())); changed = true;
            }
            if (oldId != newId) {
                manager.updateFarm(oldId, 7, Integer.toString(newId)); changed = true;
            }
            if (!old.getLastUpdated().equals(edited.getLastUpdated())) {
                changed = true;
            }
            if (!changed) {
                setStatus("No changes to save.");
                return;
            }

            // set the date last so the date shown in the form is accurate.
            manager.updateFarm(newId, 8, edited.getLastUpdated().toString());
            refreshTable(manager.getAllFarms());
            selectFarm(newId);
            setStatus("✓ " + edited.getName() + " updated.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void deleteFarm() {
        Farm farm = selectedFarm();
        if (farm == null) {
            // farms can also be deleted by typing its id on the form.
            try {
                int id = Integer.parseInt(idField.getText().trim());
                farm = manager.findFarmById(id);
            } catch (NumberFormatException exception) {
                showError("Select a farm or enter its ID to delete it.");
                return;
            }
            if (farm == null) {
                showError("No farm exists with that ID.");
                return;
            }
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + farm.getName() + " (ID " + farm.getFarmId() + ")?",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        manager.removeFarm(farm.getFarmId());
        refreshTable(manager.getAllFarms());
        setStatus("✓ Farm " + farm.getFarmId() + " deleted.");
    }

    private void findLocalFarms() {
        String product = productSearch.getText().trim();
        if (!validator.isNotBlank(product)) {
            showError("Enter a product to search for.");
            return;
        }
        try {
            double miles = Double.parseDouble(maxMilesSearch.getText().trim());
            if (!validator.isValidDistance(miles)) {
                throw new IllegalArgumentException("Max miles must be between 0 and 200.");
            }
            boolean onlyOrganic = searchOrganic.isSelected();
            ArrayList<Farm> matches = manager.findLocalFarms(product, miles, onlyOrganic);
            refreshTable(matches);
            if (!matches.isEmpty()) {
                selectFarm(matches.get(0).getFarmId());
                setStatus("✓ " + matches.size() + " match(es) | Average distance: "
                        + String.format(Locale.US, "%.2f", manager.calculateAverageDistance(matches)) + " miles.");
            } else if (onlyOrganic && !manager.findLocalFarms(product, miles, false).isEmpty()) {
                setStatus("No organic match. Uncheck Organic only to include other farms.");
            } else {
                setStatus("No farms found for '" + product + "' within " + miles + " miles.");
            }
        } catch (NumberFormatException exception) {
            showError("Max miles must be a number between 0 and 200.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a farm text file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        if (manager.getFarmCount() > 0 && JOptionPane.showConfirmDialog(this,
                "Loading a file replaces the farms currently in memory. Continue?",
                "Load farms", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        File chosenFile = chooser.getSelectedFile();
        int count = manager.loadFromFile(chosenFile.getAbsolutePath());
        if (count < 0) {
            showError(manager.getLastFileError());
            return;
        }
        filePath = chosenFile.getAbsolutePath();
        refreshTable(manager.getAllFarms());
        if (count > 0) selectFarm(displayedFarms.get(0).getFarmId());
        setStatus("✓ Loaded " + count + " farm(s). Skipped lines: "
                + manager.getLastSkippedLines() + ".");
    }

    // clear the search filters and restore the complete farm list.
    private void clearSearch() {
        Farm selected = selectedFarm();
        int selectedId = selected == null ? -1 : selected.getFarmId();
        productSearch.setText("");
        maxMilesSearch.setText("15");
        searchOrganic.setSelected(true);
        searchButton.setText("Find Local Organic");
        refreshTable(manager.getAllFarms());
        if (selectedId > 0) {
            selectFarm(selectedId);
        }
        setStatus("Showing all " + manager.getFarmCount() + " farm(s). Search cleared.");
    }

    // the clear button only clears the farm details form.
    private void clearDetailsForm() {
        table.clearSelection();
        clearForm();
        setStatus("Farm details cleared. Ready to add a new farm.");
    }

    private void saveAndExit() {
        if (manager.getFarmCount() == 0 && JOptionPane.showConfirmDialog(this,
                "No farms are loaded. Save an empty file and exit?",
                "Save & Exit", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (!manager.saveToFile(filePath)) {
            showError(manager.getLastFileError() + " Your farms are still in memory.");
            return;
        }
        JOptionPane.showMessageDialog(this, "Saved " + manager.getFarmCount()
                + " farm(s) to " + filePath + ".", "Saved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void setStatus(String message) {
        status.setForeground(DARK);
        status.setText(message);
    }

    private void showError(String message) {
        status.setForeground(new Color(150, 54, 41));
        status.setText("⚠ " + message);
        JOptionPane.showMessageDialog(this, message, "Check input", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FarmGUI().setVisible(true));
    }
}
