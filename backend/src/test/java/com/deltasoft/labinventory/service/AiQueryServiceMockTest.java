package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.repository.ReagentEventRepository;
import com.deltasoft.labinventory.repository.ReagentRepository;
import com.deltasoft.labinventory.web.dto.AiQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"ai.mock=true"})
class AiQueryServiceMockTest {

    @Autowired AiQueryService service;
    @Autowired ReagentRepository repo;
    @Autowired ReagentEventRepository events;

    @BeforeEach
    void clean() {
        events.deleteAll();
        repo.deleteAll();
    }

    @Test
    void parsesExpiredFlammables() {
        Reagent r = new Reagent("OldEthanol", "Fisher", new BigDecimal("4"), "L",
                "Lab-2", LocalDate.now().minusDays(2), new BigDecimal("5"));
        r.setHazardClass(HazardClass.FLAMMABLE);
        repo.save(r);

        AiQueryResponse res = service.query("expired flammables", 25);
        assertEquals("EXPIRED", res.getFilter().getStatus());
        assertEquals("FLAMMABLE", res.getFilter().getHazardClass());
        assertNotNull(res.getInterpretation());
    }

    @Test
    void parsesLowStockPhrase() {
        AiQueryResponse res = service.query("show me low stock items", 25);
        assertEquals("LOW_STOCK", res.getFilter().getStatus());
    }

    @Test
    void parsesCorrosivesPhrase() {
        AiQueryResponse res = service.query("corrosive acids", 25);
        assertEquals("CORROSIVE", res.getFilter().getHazardClass());
    }

    @Test
    void parsesToxic() {
        AiQueryResponse res = service.query("toxic chemicals", 25);
        assertEquals("TOXIC", res.getFilter().getHazardClass());
    }

    @Test
    void parsesExpiresWithinDaysFromWeeks() {
        AiQueryResponse res = service.query("expires within 2 weeks", 25);
        assertEquals(14, res.getFilter().getExpiresWithinDays());
    }

    @Test
    void parsesExpiresWithinDaysFromMonths() {
        AiQueryResponse res = service.query("flammables that expire in 2 months", 25);
        assertEquals(60, res.getFilter().getExpiresWithinDays());
        assertEquals("FLAMMABLE", res.getFilter().getHazardClass());
    }

    @Test
    void expiringSoonDefaultsTo30() {
        AiQueryResponse res = service.query("flammables expiring soon", 25);
        assertEquals(30, res.getFilter().getExpiresWithinDays());
    }

    @Test
    void searchExtractsLabLocation() {
        AiQueryResponse res = service.query("flammables in Lab-2", 25);
        assertEquals("Lab-2", res.getFilter().getSearch());
        assertEquals("FLAMMABLE", res.getFilter().getHazardClass());
    }

    @Test
    void responseWrapsPagedReagents() {
        Reagent r = new Reagent("Methanol", "Honeywell", new BigDecimal("10"), "L",
                "Lab-2", LocalDate.now().plusYears(1), new BigDecimal("5"));
        r.setHazardClass(HazardClass.FLAMMABLE);
        repo.save(r);

        AiQueryResponse res = service.query("flammables", 25);
        assertNotNull(res.getPage());
        assertTrue(res.getPage().getTotalElements() >= 1);
        assertEquals("expirationDate,asc", res.getFilter().getSort());
        // In mock mode without an API key, no SDK call; the fallback flag stays null
        // because mock=true is the configured mode, not a degraded fallback.
        assertNull(res.getFallback());
    }
}
