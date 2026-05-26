package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.repository.ReagentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ReagentRepository repo;

    @BeforeEach
    void clean() { repo.deleteAll(); }

    @Test
    void summaryCountsAreCorrect() throws Exception {
        // 1 expired
        repo.save(new Reagent("Old", "X", new BigDecimal("100"), "g",
                "Lab-1", LocalDate.now().minusDays(1), new BigDecimal("5")));
        // 1 low stock
        repo.save(new Reagent("Low", "X", new BigDecimal("1"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("5")));
        // 2 in stock
        repo.save(new Reagent("Ok1", "X", new BigDecimal("50"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("5")));
        repo.save(new Reagent("Ok2", "X", new BigDecimal("60"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("5")));

        mvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReagents", is(4)))
                .andExpect(jsonPath("$.expiredCount", is(1)))
                .andExpect(jsonPath("$.lowStockCount", is(1)))
                .andExpect(jsonPath("$.inStockCount", is(2)));
    }
}
