package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.service.ReagentService;
import com.deltasoft.labinventory.web.dto.DashboardSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReagentService service;

    public DashboardController(ReagentService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return service.summary();
    }
}
