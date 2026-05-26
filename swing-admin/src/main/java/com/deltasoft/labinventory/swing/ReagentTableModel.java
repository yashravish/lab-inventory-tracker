package com.deltasoft.labinventory.swing;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ReagentTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "ID", "Name", "Supplier", "Lot #", "CAS", "Quantity", "Unit",
        "Storage Location", "Expiration Date", "Status"
    };

    private List<ReagentDto> rows = new ArrayList<>();

    public void setRows(List<ReagentDto> rows) {
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public ReagentDto getRow(int index) {
        return rows.get(index);
    }

    public int rowSize() { return rows.size(); }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int c) { return COLUMNS[c]; }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int r, int c) { return false; }

    @Override
    public Object getValueAt(int r, int c) {
        ReagentDto row = rows.get(r);
        return switch (c) {
            case 0 -> row.id == null ? "" : row.id.toString();
            case 1 -> row.name == null ? "" : row.name;
            case 2 -> row.supplier == null ? "" : row.supplier;
            case 3 -> row.lotNumber == null ? "" : row.lotNumber;
            case 4 -> row.casNumber == null ? "" : row.casNumber;
            case 5 -> row.quantity == null ? "" : row.quantity.toPlainString();
            case 6 -> row.unit == null ? "" : row.unit;
            case 7 -> row.storageLocation == null ? "" : row.storageLocation;
            case 8 -> row.expirationDate == null ? "" : row.expirationDate.toString();
            case 9 -> row.status == null ? "" : row.status;
            default -> "";
        };
    }
}
