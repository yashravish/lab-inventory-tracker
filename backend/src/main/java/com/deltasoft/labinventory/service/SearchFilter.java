package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.ReagentStatus;

public record SearchFilter(
        String search,
        ReagentStatus status,
        HazardClass hazardClass,
        Integer expiresWithinDays
) {
    public static SearchFilter empty() {
        return new SearchFilter(null, null, null, null);
    }

    public boolean hasPostFilters() {
        return status != null || hazardClass != null || expiresWithinDays != null;
    }
}
