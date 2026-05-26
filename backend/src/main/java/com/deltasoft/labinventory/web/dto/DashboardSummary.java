package com.deltasoft.labinventory.web.dto;

public class DashboardSummary {
    private long totalReagents;
    private long lowStockCount;
    private long expiredCount;
    private long inStockCount;

    public DashboardSummary() {}

    public DashboardSummary(long total, long lowStock, long expired, long inStock) {
        this.totalReagents = total;
        this.lowStockCount = lowStock;
        this.expiredCount = expired;
        this.inStockCount = inStock;
    }

    public long getTotalReagents() { return totalReagents; }
    public long getLowStockCount() { return lowStockCount; }
    public long getExpiredCount() { return expiredCount; }
    public long getInStockCount() { return inStockCount; }

    public void setTotalReagents(long v) { this.totalReagents = v; }
    public void setLowStockCount(long v) { this.lowStockCount = v; }
    public void setExpiredCount(long v) { this.expiredCount = v; }
    public void setInStockCount(long v) { this.inStockCount = v; }
}
