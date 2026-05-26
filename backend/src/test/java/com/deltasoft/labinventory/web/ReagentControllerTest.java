package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.domain.ReagentEvent;
import com.deltasoft.labinventory.repository.ReagentEventRepository;
import com.deltasoft.labinventory.repository.ReagentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReagentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ReagentRepository repo;
    @Autowired ReagentEventRepository eventRepo;

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void clean() {
        eventRepo.deleteAll();
        repo.deleteAll();
    }

    @Test
    void listReturnsCreatedReagents() throws Exception {
        repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));

        mvc.perform(get("/api/reagents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Ethanol")))
                .andExpect(jsonPath("$.content[0].status", is("IN_STOCK")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    void postCreatesValidReagent() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Methanol",
                "supplier", "Honeywell",
                "quantity", "8.5",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusYears(1).toString(),
                "minimumQuantity", "2",
                "lotNumber", "LOT-2024-M001",
                "casNumber", "67-56-1"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Methanol")))
                .andExpect(jsonPath("$.lotNumber", is("LOT-2024-M001")))
                .andExpect(jsonPath("$.casNumber", is("67-56-1")))
                .andExpect(jsonPath("$.status", is("IN_STOCK")));
    }

    @Test
    void postAcceptsMissingLotAndCasNumbers() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Generic",
                "quantity", "1",
                "minimumQuantity", "0"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotNumber", nullValue()))
                .andExpect(jsonPath("$.casNumber", nullValue()));
    }

    @Test
    void postRejectsInvalidCasNumber() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "BadCas",
                "quantity", "1",
                "minimumQuantity", "0",
                "casNumber", "not-a-cas"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.casNumber", notNullValue()));
    }

    @Test
    void postRejectsNegativeQuantity() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "BadStuff",
                "quantity", "-1",
                "minimumQuantity", "0"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.quantity", notNullValue()));
    }

    @Test
    void putUpdatesQuantity() throws Exception {
        Reagent r = repo.save(new Reagent("Acetone", "VWR", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));

        Map<String, Object> body = Map.of(
                "name", "Acetone",
                "supplier", "VWR",
                "quantity", "2",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusMonths(6).toString(),
                "minimumQuantity", "5"
        );
        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(2)))
                .andExpect(jsonPath("$.status", is("LOW_STOCK")));
    }

    @Test
    void deleteRemovesReagent() throws Exception {
        Reagent r = repo.save(new Reagent("Glucose", "Thermo", new BigDecimal("100"), "g",
                "Lab-1", LocalDate.now().plusYears(2), new BigDecimal("10")));

        mvc.perform(delete("/api/reagents/" + r.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/reagents/" + r.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchFiltersByNameOrSupplier() throws Exception {
        repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L", "Lab-2",
                LocalDate.now().plusYears(1), new BigDecimal("5")));
        repo.save(new Reagent("Glucose", "Thermo", new BigDecimal("100"), "g", "Lab-1",
                LocalDate.now().plusYears(1), new BigDecimal("5")));

        mvc.perform(get("/api/reagents").param("search", "thermo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Glucose")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void postRoundTripsHazardClass() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Ethanol",
                "supplier", "Fisher",
                "quantity", "10",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusYears(1).toString(),
                "minimumQuantity", "5",
                "hazardClass", "FLAMMABLE"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hazardClass", is("FLAMMABLE")));
    }

    @Test
    void postWithoutHazardClassDefaultsToNone() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Plain Salt",
                "quantity", "100",
                "minimumQuantity", "5"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hazardClass", is("NONE")));
    }

    @Test
    void putUpdatesHazardClass() throws Exception {
        Reagent r = repo.save(new Reagent("Acetone", "VWR", new BigDecimal("3"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));

        Map<String, Object> body = Map.of(
                "name", "Acetone",
                "supplier", "VWR",
                "quantity", "3",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusMonths(6).toString(),
                "minimumQuantity", "5",
                "hazardClass", "CORROSIVE"
        );
        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hazardClass", is("CORROSIVE")));
    }

    @Test
    void statusReflectsExpiredOverLowStock() throws Exception {
        repo.save(new Reagent("Old Buffer", "Bio-Rad", new BigDecimal("0.1"), "L",
                "Lab-4", LocalDate.now().minusDays(1), new BigDecimal("5")));

        mvc.perform(get("/api/reagents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status", is("EXPIRED")))
                .andExpect(jsonPath("$.content[0].expired", is(true)));
    }

    @Test
    void sortByQuantityDescReordersResults() throws Exception {
        repo.save(new Reagent("Alpha", "S1", new BigDecimal("3"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("1")));
        repo.save(new Reagent("Bravo", "S2", new BigDecimal("100"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("1")));
        repo.save(new Reagent("Charlie", "S3", new BigDecimal("50"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("1")));

        mvc.perform(get("/api/reagents").param("sort", "quantity,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].name", is("Bravo")))
                .andExpect(jsonPath("$.content[1].name", is("Charlie")))
                .andExpect(jsonPath("$.content[2].name", is("Alpha")));
    }

    @Test
    void unknownSortPropertyReturns400() throws Exception {
        mvc.perform(get("/api/reagents").param("sort", "foo,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagingHonoursPageAndSize() throws Exception {
        for (int i = 0; i < 5; i++) {
            repo.save(new Reagent("R" + i, "S", new BigDecimal(i + 1), "g",
                    "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("0")));
        }

        mvc.perform(get("/api/reagents").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void putWithStaleVersionReturns409() throws Exception {
        Reagent r = repo.save(new Reagent("Buffer", "Bio-Rad", new BigDecimal("4"), "L",
                "Lab-4", LocalDate.now().plusMonths(6), new BigDecimal("2")));
        Long current = r.getVersion();

        Map<String, Object> body = Map.of(
                "name", "Buffer",
                "supplier", "Bio-Rad",
                "quantity", "1",
                "unit", "L",
                "storageLocation", "Lab-4",
                "expirationDate", LocalDate.now().plusMonths(6).toString(),
                "minimumQuantity", "2",
                "version", current + 999
        );
        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("modified by another user")))
                .andExpect(jsonPath("$.currentVersion", is(current.intValue())));
    }

    @Test
    void putWithCurrentVersionSucceedsAndIncrementsVersion() throws Exception {
        Reagent r = repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));
        Long current = r.getVersion();

        Map<String, Object> body = Map.of(
                "name", "Ethanol",
                "supplier", "Fisher",
                "quantity", "8",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusMonths(6).toString(),
                "minimumQuantity", "5",
                "version", current
        );
        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(8)))
                .andExpect(jsonPath("$.version", is(current.intValue() + 1)));
    }

    @Test
    void createWritesCreatedEvent() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Methanol",
                "quantity", "5",
                "minimumQuantity", "1"
        );
        mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());

        List<ReagentEvent> all = eventRepo.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());
        ReagentEvent e = all.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(ReagentEvent.EventType.CREATED, e.getEventType());
        org.junit.jupiter.api.Assertions.assertEquals("Methanol", e.getReagentName());
        org.junit.jupiter.api.Assertions.assertNotNull(e.getActor());
        org.junit.jupiter.api.Assertions.assertNotNull(e.getCreatedAt());
    }

    @Test
    void updateWritesUpdatedEventWithDiff() throws Exception {
        Reagent r = repo.save(new Reagent("Acetone", "VWR", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));

        Map<String, Object> body = Map.of(
                "name", "Acetone",
                "supplier", "VWR",
                "quantity", "2",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusMonths(6).toString(),
                "minimumQuantity", "5"
        );
        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/reagents/" + r.getId() + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType", is("UPDATED")))
                .andExpect(jsonPath("$[0].reagentId", is(r.getId().intValue())))
                .andExpect(jsonPath("$[0].changes.quantity[1]", is("2")))
                .andExpect(jsonPath("$[0].changes.quantity[0]", notNullValue()))
                .andExpect(jsonPath("$[0].changes.name").doesNotExist());
    }

    @Test
    void deleteWritesDeletedEventAndHistoryStillReadable() throws Exception {
        Reagent r = repo.save(new Reagent("Glucose", "Thermo", new BigDecimal("100"), "g",
                "Lab-1", LocalDate.now().plusYears(2), new BigDecimal("10")));
        Long id = r.getId();

        mvc.perform(delete("/api/reagents/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/reagents/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType", is("DELETED")))
                .andExpect(jsonPath("$[0].reagentName", is("Glucose")));
    }

    @Test
    void historyReturnsEventsInDescendingOrder() throws Exception {
        Map<String, Object> createBody = Map.of(
                "name", "Buffer",
                "quantity", "10",
                "minimumQuantity", "1"
        );
        var created = mvc.perform(post("/api/reagents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = ((Number) json.readValue(
                created.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        Map<String, Object> updateBody = Map.of(
                "name", "Buffer",
                "quantity", "3",
                "minimumQuantity", "1"
        );
        mvc.perform(put("/api/reagents/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(updateBody)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/reagents/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventType", is("UPDATED")))
                .andExpect(jsonPath("$[1].eventType", is("CREATED")));
    }

    @Test
    void updateWithNoFieldChangesWritesNoEvent() throws Exception {
        Reagent r = repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));

        Map<String, Object> body = Map.of(
                "name", "Ethanol",
                "supplier", "Fisher",
                "quantity", "10",
                "unit", "L",
                "storageLocation", "Lab-2",
                "expirationDate", LocalDate.now().plusMonths(6).toString(),
                "minimumQuantity", "5"
        );
        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());

        long updates = eventRepo.findByReagentIdOrderByCreatedAtDesc(r.getId()).stream()
                .filter(e -> e.getEventType() == ReagentEvent.EventType.UPDATED)
                .count();
        org.junit.jupiter.api.Assertions.assertEquals(0, updates);
    }

    @Test
    void filterByStatusExpiredReturnsOnlyExpiredRows() throws Exception {
        repo.save(new Reagent("Old Buffer", "Bio-Rad", new BigDecimal("0.1"), "L",
                "Lab-4", LocalDate.now().minusDays(1), new BigDecimal("5")));
        repo.save(new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusYears(1), new BigDecimal("5")));

        mvc.perform(get("/api/reagents").param("status", "EXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Old Buffer")))
                .andExpect(jsonPath("$.content[0].status", is("EXPIRED")));
    }

    @Test
    void filterByHazardClassFlammableReturnsOnlyFlammableRows() throws Exception {
        Reagent eth = new Reagent("Ethanol", "Fisher", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusYears(1), new BigDecimal("5"));
        eth.setHazardClass(HazardClass.FLAMMABLE);
        repo.save(eth);

        Reagent glu = new Reagent("Glucose", "Thermo", new BigDecimal("100"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("5"));
        glu.setHazardClass(HazardClass.NONE);
        repo.save(glu);

        mvc.perform(get("/api/reagents").param("hazardClass", "FLAMMABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Ethanol")))
                .andExpect(jsonPath("$.content[0].hazardClass", is("FLAMMABLE")));
    }

    @Test
    void filterByExpiresWithinDaysIncludesUpcomingAndExcludesExpired() throws Exception {
        // expires in 10 days — included
        repo.save(new Reagent("SoonExpires", "S", new BigDecimal("5"), "L",
                "Lab-1", LocalDate.now().plusDays(10), new BigDecimal("1")));
        // expires in 90 days — excluded by 30-day window
        repo.save(new Reagent("LaterExpires", "S", new BigDecimal("5"), "L",
                "Lab-1", LocalDate.now().plusDays(90), new BigDecimal("1")));
        // already expired — excluded
        repo.save(new Reagent("OldStuff", "S", new BigDecimal("5"), "L",
                "Lab-1", LocalDate.now().minusDays(2), new BigDecimal("1")));

        mvc.perform(get("/api/reagents").param("expiresWithinDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("SoonExpires")));
    }

    @Test
    void filterByUnknownStatusReturns400() throws Exception {
        mvc.perform(get("/api/reagents").param("status", "GARBAGE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterByUnknownHazardClassReturns400() throws Exception {
        mvc.perform(get("/api/reagents").param("hazardClass", "RADIOACTIVE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterByNegativeExpiresWithinDaysReturns400() throws Exception {
        mvc.perform(get("/api/reagents").param("expiresWithinDays", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void combinedFiltersAndNarrow() throws Exception {
        // Flammable + low stock — matches both
        Reagent a = new Reagent("Methanol", "Honeywell", new BigDecimal("0.1"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5"));
        a.setHazardClass(HazardClass.FLAMMABLE);
        repo.save(a);
        // Flammable but not low stock
        Reagent b = new Reagent("Ethanol", "Fisher", new BigDecimal("100"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5"));
        b.setHazardClass(HazardClass.FLAMMABLE);
        repo.save(b);
        // Low stock but not flammable
        Reagent c = new Reagent("Glucose", "Thermo", new BigDecimal("0.1"), "g",
                "Lab-1", LocalDate.now().plusYears(1), new BigDecimal("5"));
        c.setHazardClass(HazardClass.NONE);
        repo.save(c);

        mvc.perform(get("/api/reagents")
                        .param("status", "LOW_STOCK")
                        .param("hazardClass", "FLAMMABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Methanol")));
    }

    @Test
    void putWithNullVersionStillSucceeds() throws Exception {
        Reagent r = repo.save(new Reagent("Acetone", "VWR", new BigDecimal("3"), "L",
                "Lab-2", LocalDate.now().plusMonths(6), new BigDecimal("5")));

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", "Acetone");
        body.put("supplier", "VWR");
        body.put("quantity", "2");
        body.put("unit", "L");
        body.put("storageLocation", "Lab-2");
        body.put("expirationDate", LocalDate.now().plusMonths(6).toString());
        body.put("minimumQuantity", "5");
        body.put("version", null);

        mvc.perform(put("/api/reagents/" + r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(2)));
    }
}
