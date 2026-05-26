package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.repository.ReagentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReagentCsvService {

    static final String[] HEADERS = {
            "name", "supplier", "quantity", "unit", "storageLocation", "expirationDate",
            "minimumQuantity", "lotNumber", "casNumber", "hazardClass"
    };

    private final ReagentRepository repo;

    public ReagentCsvService(ReagentRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public void exportTo(Writer writer) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .build();
        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (Reagent r : repo.findAll(Sort.by(Sort.Direction.ASC, "name"))) {
                printer.printRecord(
                        r.getName(),
                        nullToBlank(r.getSupplier()),
                        r.getQuantity(),
                        nullToBlank(r.getUnit()),
                        nullToBlank(r.getStorageLocation()),
                        r.getExpirationDate() == null ? "" : r.getExpirationDate().toString(),
                        r.getMinimumQuantity(),
                        nullToBlank(r.getLotNumber()),
                        nullToBlank(r.getCasNumber()),
                        r.getHazardClass() == null ? "" : r.getHazardClass().name()
                );
            }
            printer.flush();
        }
    }

    @Transactional
    public ImportResult importFrom(InputStream in) throws IOException {
        ImportResult result = new ImportResult();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            int rowNum = 0;
            for (CSVRecord rec : parser) {
                rowNum++;
                try {
                    String name = readField(rec, "name");
                    String quantityStr = readField(rec, "quantity");

                    if (name == null || name.isBlank()) {
                        result.skipped++;
                        result.errors.add(new ImportError(rowNum, "missing required field: name"));
                        continue;
                    }
                    if (quantityStr == null || quantityStr.isBlank()) {
                        result.skipped++;
                        result.errors.add(new ImportError(rowNum, "missing required field: quantity"));
                        continue;
                    }

                    BigDecimal quantity;
                    try {
                        quantity = new BigDecimal(quantityStr);
                    } catch (NumberFormatException nfe) {
                        result.skipped++;
                        result.errors.add(new ImportError(rowNum, "invalid quantity: " + quantityStr));
                        continue;
                    }
                    if (quantity.signum() < 0) {
                        result.skipped++;
                        result.errors.add(new ImportError(rowNum, "quantity must be >= 0"));
                        continue;
                    }

                    Optional<Reagent> existing = repo.findFirstByNameIgnoreCase(name);
                    Reagent r = existing.orElseGet(Reagent::new);
                    r.setName(name);
                    r.setSupplier(readField(rec, "supplier"));
                    r.setQuantity(quantity);
                    r.setUnit(readField(rec, "unit"));
                    r.setStorageLocation(readField(rec, "storageLocation"));

                    String exp = readField(rec, "expirationDate");
                    r.setExpirationDate(exp == null ? null : LocalDate.parse(exp));

                    String minQty = readField(rec, "minimumQuantity");
                    if (minQty != null) {
                        r.setMinimumQuantity(new BigDecimal(minQty));
                    } else if (r.getMinimumQuantity() == null) {
                        r.setMinimumQuantity(new BigDecimal("5"));
                    }

                    r.setLotNumber(readField(rec, "lotNumber"));
                    r.setCasNumber(readField(rec, "casNumber"));

                    String hz = readField(rec, "hazardClass");
                    if (hz != null) {
                        try {
                            r.setHazardClass(HazardClass.valueOf(hz.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            r.setHazardClass(HazardClass.NONE);
                        }
                    } else if (r.getHazardClass() == null) {
                        r.setHazardClass(HazardClass.NONE);
                    }

                    repo.save(r);
                    if (existing.isPresent()) result.updated++;
                    else result.created++;

                } catch (Exception e) {
                    result.skipped++;
                    String msg = e.getMessage();
                    result.errors.add(new ImportError(rowNum, msg == null ? e.getClass().getSimpleName() : msg));
                }
            }
        }
        return result;
    }

    private static String readField(CSVRecord rec, String name) {
        if (!rec.isMapped(name)) return null;
        String v = rec.get(name);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static String nullToBlank(String s) {
        return s == null ? "" : s;
    }

    public static class ImportResult {
        private int created;
        private int updated;
        private int skipped;
        private final List<ImportError> errors = new ArrayList<>();

        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public List<ImportError> getErrors() { return errors; }
    }

    public static class ImportError {
        private final int row;
        private final String message;

        public ImportError(int row, String message) {
            this.row = row;
            this.message = message;
        }

        public int getRow() { return row; }
        public String getMessage() { return message; }
    }
}
