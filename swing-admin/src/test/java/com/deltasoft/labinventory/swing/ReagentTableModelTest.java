package com.deltasoft.labinventory.swing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReagentTableModelTest {

    @Test
    void parsesAndRendersRowsFromJson() throws Exception {
        String json = """
            [
              {
                "id": 1, "name": "Ethanol", "supplier": "Fisher",
                "quantity": 12.5, "unit": "L",
                "storageLocation": "Lab-2",
                "expirationDate": "2027-01-01",
                "minimumQuantity": 5,
                "lotNumber": "LOT-2024-E113",
                "casNumber": "64-17-5",
                "status": "IN_STOCK", "lowStock": false, "expired": false
              },
              {
                "id": 2, "name": "Methanol", "supplier": "Honeywell",
                "quantity": 0.5, "unit": "L",
                "storageLocation": "Lab-2",
                "expirationDate": "2026-05-01",
                "minimumQuantity": 5,
                "lotNumber": null,
                "casNumber": null,
                "status": "LOW_STOCK", "lowStock": true, "expired": false
              }
            ]
        """;

        ReagentApiClient client = new ReagentApiClient("http://localhost:8080");
        List<ReagentDto> rows = client.parseList(json);

        ReagentTableModel model = new ReagentTableModel();
        model.setRows(rows);

        assertEquals(2, model.getRowCount());
        assertEquals(10, model.getColumnCount());

        assertEquals("1", model.getValueAt(0, 0));
        assertEquals("Ethanol", model.getValueAt(0, 1));
        assertEquals("Fisher", model.getValueAt(0, 2));
        assertEquals("LOT-2024-E113", model.getValueAt(0, 3));
        assertEquals("64-17-5", model.getValueAt(0, 4));
        assertEquals("12.5", model.getValueAt(0, 5));
        assertEquals("L", model.getValueAt(0, 6));
        assertEquals("Lab-2", model.getValueAt(0, 7));
        assertEquals("2027-01-01", model.getValueAt(0, 8));
        assertEquals("IN_STOCK", model.getValueAt(0, 9));

        assertEquals("", model.getValueAt(1, 3));
        assertEquals("", model.getValueAt(1, 4));
        assertEquals("LOW_STOCK", model.getValueAt(1, 9));
    }

    @Test
    void handlesNullFieldsGracefully() {
        ReagentDto dto = new ReagentDto();
        dto.id = 42L;
        dto.name = "X";
        ReagentTableModel model = new ReagentTableModel();
        model.setRows(List.of(dto));

        for (int c = 0; c < model.getColumnCount(); c++) {
            assertNotNull(model.getValueAt(0, c), "column " + c + " should not be null");
        }
        assertEquals("42", model.getValueAt(0, 0));
        assertEquals("X", model.getValueAt(0, 1));
        assertEquals("", model.getValueAt(0, 2));
    }

    @Test
    void columnNamesIncludeExpectedHeaders() {
        ReagentTableModel model = new ReagentTableModel();
        assertEquals("ID", model.getColumnName(0));
        assertEquals("Name", model.getColumnName(1));
        assertEquals("Supplier", model.getColumnName(2));
        assertEquals("Lot #", model.getColumnName(3));
        assertEquals("CAS", model.getColumnName(4));
        assertEquals("Status", model.getColumnName(9));
    }

    @Test
    void unusedDtoFieldsCompile() {
        ReagentDto dto = new ReagentDto();
        dto.quantity = new BigDecimal("1");
        dto.expirationDate = LocalDate.now();
        dto.minimumQuantity = new BigDecimal("2");
        dto.lotNumber = "LOT-X";
        dto.casNumber = "1-2-3";
        assertEquals(LocalDate.now(), dto.expirationDate);
        assertEquals("LOT-X", dto.lotNumber);
    }

    @Test
    void parsesPageShapeJsonViaParsePage() throws Exception {
        String json = """
            {
              "content": [
                {
                  "id": 10, "name": "Acetone", "supplier": "VWR",
                  "quantity": 4.0, "unit": "L",
                  "storageLocation": "Lab-3",
                  "expirationDate": "2027-06-01",
                  "minimumQuantity": 5,
                  "status": "LOW_STOCK", "lowStock": true, "expired": false
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "number": 0,
              "size": 25
            }
        """;

        ReagentApiClient client = new ReagentApiClient("http://localhost:8080");
        List<ReagentDto> rows = client.parsePage(json);

        assertEquals(1, rows.size());
        assertEquals("Acetone", rows.get(0).name);
        assertEquals("LOW_STOCK", rows.get(0).status);
    }

    @Test
    void clientWithCredsExposesBasicAuthHeader() {
        ReagentApiClient client = new ReagentApiClient("http://localhost:8080", "yash.s", "labtech");
        String header = client.authorizationHeader();
        assertNotNull(header);
        assertTrue(header.startsWith("Basic "), "expected Basic prefix, got: " + header);
        // "yash.s:labtech" base64-encoded
        assertEquals("Basic eWFzaC5zOmxhYnRlY2g=", header);
    }

    @Test
    void clientWithoutCredsHasNoAuthHeader() {
        ReagentApiClient client = new ReagentApiClient("http://localhost:8080");
        assertNull(client.authorizationHeader());
    }

    @Test
    void parsesHazardClassFromJson() throws Exception {
        String json = """
            [
              {
                "id": 1, "name": "Ethanol", "supplier": "Fisher",
                "quantity": 12.5, "unit": "L",
                "storageLocation": "Lab-2",
                "expirationDate": "2027-01-01",
                "minimumQuantity": 5,
                "status": "IN_STOCK", "lowStock": false, "expired": false,
                "hazardClass": "FLAMMABLE"
              },
              {
                "id": 2, "name": "Glucose", "supplier": "Thermo",
                "quantity": 100, "unit": "g",
                "storageLocation": "Lab-1",
                "expirationDate": "2027-01-01",
                "minimumQuantity": 5,
                "status": "IN_STOCK", "lowStock": false, "expired": false,
                "hazardClass": "NONE"
              }
            ]
        """;

        ReagentApiClient client = new ReagentApiClient("http://localhost:8080");
        List<ReagentDto> rows = client.parseList(json);

        assertEquals(2, rows.size());
        assertEquals("FLAMMABLE", rows.get(0).hazardClass);
        assertEquals("NONE", rows.get(1).hazardClass);
    }
}
