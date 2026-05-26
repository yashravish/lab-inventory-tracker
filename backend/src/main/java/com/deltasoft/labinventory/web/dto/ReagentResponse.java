package com.deltasoft.labinventory.web.dto;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.domain.ReagentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReagentResponse {

    private Long id;
    private String name;
    private String supplier;
    private BigDecimal quantity;
    private String unit;
    private String storageLocation;
    private LocalDate expirationDate;
    private BigDecimal minimumQuantity;
    private String lotNumber;
    private String casNumber;
    private HazardClass hazardClass;
    private Long version;
    private boolean lowStock;
    private boolean expired;
    private ReagentStatus status;

    public static ReagentResponse from(Reagent r) {
        ReagentResponse dto = new ReagentResponse();
        dto.id = r.getId();
        dto.name = r.getName();
        dto.supplier = r.getSupplier();
        dto.quantity = r.getQuantity();
        dto.unit = r.getUnit();
        dto.storageLocation = r.getStorageLocation();
        dto.expirationDate = r.getExpirationDate();
        dto.minimumQuantity = r.getMinimumQuantity();
        dto.lotNumber = r.getLotNumber();
        dto.casNumber = r.getCasNumber();
        dto.hazardClass = r.getHazardClass();
        dto.version = r.getVersion();
        dto.lowStock = r.isLowStock();
        dto.expired = r.isExpired();
        dto.status = r.computeStatus();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSupplier() { return supplier; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getStorageLocation() { return storageLocation; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public BigDecimal getMinimumQuantity() { return minimumQuantity; }
    public String getLotNumber() { return lotNumber; }
    public String getCasNumber() { return casNumber; }
    public HazardClass getHazardClass() { return hazardClass; }
    public Long getVersion() { return version; }
    public boolean isLowStock() { return lowStock; }
    public boolean isExpired() { return expired; }
    public ReagentStatus getStatus() { return status; }
}
