package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.repository.ReagentEventRepository;
import com.deltasoft.labinventory.repository.ReagentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {"ai.mock=true"})
class AiQueryControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ReagentRepository repo;
    @Autowired ReagentEventRepository events;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void clean() {
        events.deleteAll();
        repo.deleteAll();
    }

    @Test
    void expiredFlammablesReturnsCorrectFilterAndPagedResults() throws Exception {
        Reagent r = new Reagent("OldEthanol", "Fisher", new BigDecimal("4"), "L",
                "Lab-2", LocalDate.now().minusDays(2), new BigDecimal("5"));
        r.setHazardClass(HazardClass.FLAMMABLE);
        repo.save(r);

        Reagent r2 = new Reagent("Glucose", "Thermo", new BigDecimal("100"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("5"));
        r2.setHazardClass(HazardClass.NONE);
        repo.save(r2);

        mvc.perform(post("/api/ai/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("q", "expired flammables"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.status", is("EXPIRED")))
                .andExpect(jsonPath("$.filter.hazardClass", is("FLAMMABLE")))
                .andExpect(jsonPath("$.filter.sort", is("expirationDate,asc")))
                .andExpect(jsonPath("$.interpretation", notNullValue()))
                .andExpect(jsonPath("$.page.content", hasSize(1)))
                .andExpect(jsonPath("$.page.content[0].name", is("OldEthanol")));
    }

    @Test
    void expiresWithinTwoMonthsParsesToSixtyDays() throws Exception {
        mvc.perform(post("/api/ai/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "q", "flammables expiring within 2 months",
                                "size", 50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.hazardClass", is("FLAMMABLE")))
                .andExpect(jsonPath("$.filter.expiresWithinDays", is(60)))
                .andExpect(jsonPath("$.filter.size", is(50)));
    }

    @Test
    void emptyQueryReturnsOkWithDefaultInterpretation() throws Exception {
        mvc.perform(post("/api/ai/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("q", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interpretation", notNullValue()));
    }
}
