package org.example;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DatabaseViewer extends JFrame {
    private ConnectionService connectionService;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> tableSelector;
    private JButton sortButton;
    private JTextField columnInput;
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DatabaseViewer() {
        // Set Look and Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        connectionService = new ConnectionService();
        setTitle("Database Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // Make all cells editable
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);
        table.setRowHeight(25);
        table.setSelectionBackground(new Color(184, 207, 229));
        table.setSelectionForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        tableSelector = new JComboBox<>(new String[]{"grupe", "produs", "riscuri", "stocmagazin", "vanzari"});
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(tableSelector, gbc);

        JButton loadButton = new JButton("Load Table");
        loadButton.setBackground(new Color(102, 205, 170));
        loadButton.setForeground(Color.WHITE);
        loadButton.setFocusPainted(false);
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadTableData((String) tableSelector.getSelectedItem());
            }
        });
        gbc.gridx = 1;
        controlPanel.add(loadButton, gbc);

        columnInput = new JTextField(10);
        gbc.gridx = 2;
        controlPanel.add(new JLabel("Sort Column:"), gbc);
        gbc.gridx = 3;
        controlPanel.add(columnInput, gbc);

        sortButton = new JButton("Sort by Column");
        sortButton.setBackground(new Color(72, 209, 204));
        sortButton.setForeground(Color.WHITE);
        sortButton.setFocusPainted(false);
        sortButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sortTableData();
            }
        });
        gbc.gridx = 4;
        controlPanel.add(sortButton, gbc);

        add(controlPanel, BorderLayout.NORTH);

        // Add TableModelListener to detect changes
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    if (row >= 0 && column >= 0) {
                        String columnName = tableModel.getColumnName(column);
                        Object newValue = tableModel.getValueAt(row, column);
                        Object primaryKeyValue = tableModel.getValueAt(row, 0); // Assuming first column is the primary key

                        updateDatabase(tableSelector.getSelectedItem().toString(), columnName, newValue, primaryKeyValue);
                    }
                }
            }
        });
    }

    private void loadTableData(String tableName) {
        if (!TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            JOptionPane.showMessageDialog(this, "Invalid table name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = connectionService.getConnection()) {
            System.out.println("Database connected!");

            String query = "SELECT * FROM " + tableName;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                System.out.println("Executing query: " + query);

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                System.out.println("Number of columns: " + columnCount);

                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);

                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    columnNames.add(columnName);
                    tableModel.addColumn(columnName);
                    System.out.println("Added column: " + columnName);
                }

                while (rs.next()) {
                    Object[] row = new Object[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        row[i - 1] = rs.getObject(i);
                    }
                    tableModel.addRow(row);
                    System.out.println("Added row: " + java.util.Arrays.toString(row));
                }

                if (tableModel.getRowCount() == 0) {
                    System.out.println("No data available in the table.");
                    JOptionPane.showMessageDialog(this, "No data available in the table.", "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.out.println("Data loaded successfully.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDatabase(String tableName, String columnName, Object newValue, Object primaryKeyValue) {
        if (!TABLE_NAME_PATTERN.matcher(columnName).matches()) {
            JOptionPane.showMessageDialog(this, "Invalid column name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String primaryKeyColumnName = tableModel.getColumnName(0); // Assuming the first column is the primary key

        String query = "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + primaryKeyColumnName + " = ?";

        try (Connection conn = connectionService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setObject(1, newValue);
            pstmt.setObject(2, primaryKeyValue);
            pstmt.executeUpdate();
            System.out.println("Updated database: " + query);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sortTableData() {
        String columnInputText = columnInput.getText().trim();

        if (!TABLE_NAME_PATTERN.matcher(columnInputText).matches()) {
            JOptionPane.showMessageDialog(this, "Invalid column name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int columnIndexToSort;
        try {
            columnIndexToSort = table.getColumnModel().getColumnIndex(columnInputText);
            if (columnIndexToSort == -1) {
                throw new IllegalArgumentException("Column not found: " + columnInputText);
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Error sorting data: Column not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(sorter);
            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(columnIndexToSort, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            sorter.sort();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Error sorting data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
