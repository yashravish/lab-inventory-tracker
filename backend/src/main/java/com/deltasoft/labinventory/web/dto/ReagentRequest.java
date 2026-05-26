package com.deltasoft.labinventory.web.dto;

import com.deltasoft.labinventory.domain.HazardClass;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReagentRequest {

    @NotBlank
    private String name;
    private String supplier;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "quantity must be >= 0")
    private BigDecimal quantity;

    private String unit;
    private String storageLocation;
    private LocalDate expirationDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "minimumQuantity must be >= 0")
    private BigDecimal minimumQuantity;

    private String lotNumber;

    @Pattern(regexp = "^$|^\\d{2,7}-\\d{2}-\\d$", message = "casNumber must look like 64-17-5")
    private String casNumber;

    private HazardClass hazardClass;

    private Long version;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public BigDecimal getMinimumQuantity() { return minimumQuantity; }
    public void setMinimumQuantity(BigDecimal minimumQuantity) { this.minimumQuantity = minimumQuantity; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
    public String getCasNumber() { return casNumber; }
    public void setCasNumber(String casNumber) { this.casNumber = casNumber; }
    public HazardClass getHazardClass() { return hazardClass; }
    public void setHazardClass(HazardClass hazardClass) { this.hazardClass = hazardClass; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
