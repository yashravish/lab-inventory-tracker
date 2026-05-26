package com.deltasoft.labinventory.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reagents")
public class Reagent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String supplier;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    private String unit;

    @Column(name = "storage_location")
    private String storageLocation;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "minimum_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal minimumQuantity = new BigDecimal("5");

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "cas_number")
    private String casNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "hazard_class", nullable = false, length = 16)
    private HazardClass hazardClass = HazardClass.NONE;

    @Version
    @Column(nullable = false)
    private Long version;

    public Reagent() {}

    public Reagent(String name, String supplier, BigDecimal quantity, String unit,
                   String storageLocation, LocalDate expirationDate, BigDecimal minimumQuantity) {
        this.name = name;
        this.supplier = supplier;
        this.quantity = quantity;
        this.unit = unit;
        this.storageLocation = storageLocation;
        this.expirationDate = expirationDate;
        if (minimumQuantity != null) {
            this.minimumQuantity = minimumQuantity;
        }
    }

    @Transient
    public boolean isLowStock() {
        return quantity != null && minimumQuantity != null
                && quantity.compareTo(minimumQuantity) <= 0;
    }

    @Transient
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    @Transient
    public ReagentStatus computeStatus() {
        if (isExpired()) return ReagentStatus.EXPIRED;
        if (isLowStock()) return ReagentStatus.LOW_STOCK;
        return ReagentStatus.IN_STOCK;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public void setHazardClass(HazardClass hazardClass) {
        this.hazardClass = hazardClass != null ? hazardClass : HazardClass.NONE;
    }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reagent r)) return false;
        return Objects.equals(id, r.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
