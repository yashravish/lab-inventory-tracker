package com.deltasoft.labinventory.swing;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class StatusCellRenderer extends DefaultTableCellRenderer {

    private static final Color OK_BG    = new Color(0xE8F3EC);
    private static final Color OK_FG    = new Color(0x1F6B3A);
    private static final Color WARN_BG  = new Color(0xFDF3E0);
    private static final Color WARN_FG  = new Color(0x8A5A00);
    private static final Color ERR_BG   = new Color(0xFBE9E9);
    private static final Color ERR_FG   = new Color(0x9B2C2C);

    private String status = "";

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        status = value == null ? "" : value.toString();
        if (isSelected) return c;
        setOpaque(true);
        switch (status) {
            case "EXPIRED"   -> { setBackground(ERR_BG);  setForeground(ERR_FG); }
            case "LOW_STOCK" -> { setBackground(WARN_BG); setForeground(WARN_FG); }
            case "IN_STOCK"  -> { setBackground(OK_BG);   setForeground(OK_FG); }
            default          -> { setBackground(table.getBackground()); setForeground(table.getForeground()); }
        }
        setText(label(status));
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return c;
    }

    private static String label(String s) {
        return switch (s) {
            case "IN_STOCK"  -> "In stock";
            case "LOW_STOCK" -> "Low stock";
            case "EXPIRED"   -> "Expired";
            default          -> s;
        };
    }
}
