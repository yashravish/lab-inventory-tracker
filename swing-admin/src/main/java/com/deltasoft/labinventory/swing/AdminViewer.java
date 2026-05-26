package com.deltasoft.labinventory.swing;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminViewer {

    private static final String DEFAULT_URL = "http://localhost:8082";
    private static final String DEFAULT_USER = "yash.s";
    private static final String DEFAULT_PASSWORD = "labtech";

    private final JFrame frame;
    private final JTextField searchField;
    private final JLabel lastUpdated;
    private final ReagentTableModel tableModel;
    private final JTable table;
    private final ReagentApiClient client;

    public AdminViewer(String baseUrl, String username, String password) {
        this.client = new ReagentApiClient(baseUrl, username, password);

        frame = new JFrame("Lab Inventory Admin Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1000, 600));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(200, 26));
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        toolbar.add(new JLabel("Search:"));
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);

        lastUpdated = new JLabel("Last updated: —");
        lastUpdated.setForeground(new Color(0x616E7C));
        JPanel toolbarRow = new JPanel(new BorderLayout());
        toolbarRow.add(toolbar, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.add(lastUpdated);
        toolbarRow.add(right, BorderLayout.EAST);

        tableModel = new ReagentTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setShowGrid(true);
        table.setGridColor(new Color(0xE4E7EB));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setMaxWidth(70);  // ID
        table.getColumnModel().getColumn(9).setCellRenderer(new StatusCellRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE4E7EB)));

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(toolbarRow, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> reload(null));
        searchBtn.addActionListener(e -> reload(searchField.getText()));
        searchField.addActionListener(e -> reload(searchField.getText()));
    }

    public void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        reload(null);
    }

    private void reload(String search) {
        new SwingWorker<List<ReagentDto>, Void>() {
            @Override
            protected List<ReagentDto> doInBackground() throws Exception {
                return client.list(search);
            }

            @Override
            protected void done() {
                try {
                    tableModel.setRows(get());
                    lastUpdated.setText("Last updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ReagentApiClient.UnauthorizedException) {
                        JOptionPane.showMessageDialog(frame,
                                "Authentication failed — check LAB_INVENTORY_USER / LAB_INVENTORY_PASSWORD.",
                                "Authentication failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                "Failed to load reagents:\n" + cause.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String baseUrl = System.getenv().getOrDefault("LAB_INVENTORY_API", DEFAULT_URL);
        String user = System.getenv().getOrDefault("LAB_INVENTORY_USER", DEFAULT_USER);
        String password = System.getenv().getOrDefault("LAB_INVENTORY_PASSWORD", DEFAULT_PASSWORD);
        SwingUtilities.invokeLater(() -> new AdminViewer(baseUrl, user, password).show());
    }
}
