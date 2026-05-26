package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.repository.ReagentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReagentCsvControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ReagentRepository repo;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void exportReturnsCsvWithHeaderAndSeededRow() throws Exception {
        repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("12.50"), "L",
                "Lab-2 / Flammables", LocalDate.of(2030, 1, 15), new BigDecimal("5")));

        MvcResult res = mvc.perform(get("/api/reagents/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"reagents-")))
                .andReturn();

        String body = res.getResponse().getContentAsString();
        String[] lines = body.split("\\r?\\n");
        assertThat(lines[0]).isEqualTo(
                "name,supplier,quantity,unit,storageLocation,expirationDate,"
                        + "minimumQuantity,lotNumber,casNumber,hazardClass");
        assertThat(body).contains("Ethanol,Fisher,12.5000,L,Lab-2 / Flammables,2030-01-15,5.0000,,,NONE");
    }

    @Test
    void importCreatesUpdatesAndSkipsRows() throws Exception {
        repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.of(2030, 1, 15), new BigDecimal("5")));

        String csv = ""
                + "name,supplier,quantity,unit,storageLocation,expirationDate,minimumQuantity,lotNumber,casNumber,hazardClass\n"
                + "Acetone,VWR,3,L,Lab-2,2030-06-01,5,,67-64-1,FLAMMABLE\n"
                + "ethanol,Fisher Sci,7.5,L,Lab-2,2030-01-15,5,,64-17-5,FLAMMABLE\n"
                + ",NoName,1,g,Lab-1,2030-01-01,5,,,\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "reagents.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/reagents/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(1)))
                .andExpect(jsonPath("$.updated", is(1)))
                .andExpect(jsonPath("$.skipped", is(1)))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].row", is(3)))
                .andExpect(jsonPath("$.errors[0].message", notNullValue()))
                .andExpect(jsonPath("$.errors[0].message", containsString("name")));

        Reagent updated = repo.findFirstByNameIgnoreCase("Ethanol").orElseThrow();
        assertThat(updated.getQuantity()).isEqualByComparingTo("7.5");
        assertThat(updated.getSupplier()).isEqualTo("Fisher Sci");

        Reagent created = repo.findFirstByNameIgnoreCase("Acetone").orElseThrow();
        assertThat(created.getQuantity()).isEqualByComparingTo("3");
    }
}
