package com.deltasoft.labinventory.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiQueryFilter {

    private String search;
    private String status;
    private String hazardClass;
    private Integer expiresWithinDays;
    private String sort;
    private Integer size;

    public AiQueryFilter() {}

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHazardClass() { return hazardClass; }
    public void setHazardClass(String hazardClass) { this.hazardClass = hazardClass; }
    public Integer getExpiresWithinDays() { return expiresWithinDays; }
    public void setExpiresWithinDays(Integer expiresWithinDays) { this.expiresWithinDays = expiresWithinDays; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
