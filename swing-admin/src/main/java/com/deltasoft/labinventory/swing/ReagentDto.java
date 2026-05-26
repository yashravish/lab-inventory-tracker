package com.deltasoft.labinventory.swing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReagentDto {
    public Long id;
    public String name;
    public String supplier;
    public BigDecimal quantity;
    public String unit;
    public String storageLocation;
    public LocalDate expirationDate;
    public BigDecimal minimumQuantity;
    public String lotNumber;
    public String casNumber;
    public boolean lowStock;
    public boolean expired;
    public String status;
    public String hazardClass;
}
