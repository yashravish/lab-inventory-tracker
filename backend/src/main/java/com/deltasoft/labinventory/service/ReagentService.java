package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.domain.ReagentEvent;
import com.deltasoft.labinventory.domain.ReagentStatus;
import com.deltasoft.labinventory.repository.ReagentEventRepository;
import com.deltasoft.labinventory.repository.ReagentRepository;
import com.deltasoft.labinventory.web.dto.DashboardSummary;
import com.deltasoft.labinventory.web.dto.ReagentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class ReagentService {

    private final ReagentRepository repo;
    private final ReagentEventRepository events;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReagentService(ReagentRepository repo, ReagentEventRepository events) {
        this.repo = repo;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<Reagent> list(String search, Pageable pageable) {
        return list(new SearchFilter(search, null, null, null), pageable);
    }

    // Status/hazard/expires-within filters apply to the page; for >25k rows this should become a DB-side predicate.
    @Transactional(readOnly = true)
    public Page<Reagent> list(SearchFilter filter, Pageable pageable) {
        String search = filter == null ? null : filter.search();
        Page<Reagent> page = (search == null || search.isBlank())
                ? repo.findAll(pageable)
                : repo.search(search.trim(), pageable);
        if (filter == null || !filter.hasPostFilters()) {
            return page;
        }
        LocalDate today = LocalDate.now();
        List<Reagent> filtered = page.getContent().stream()
                .filter(r -> matches(r, filter, today))
                .toList();
        return new PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    private boolean matches(Reagent r, SearchFilter f, LocalDate today) {
        if (f.status() != null && r.computeStatus() != f.status()) return false;
        if (f.hazardClass() != null && r.getHazardClass() != f.hazardClass()) return false;
        if (f.expiresWithinDays() != null) {
            LocalDate exp = r.getExpirationDate();
            if (exp == null) return false;
            if (exp.isBefore(today)) return false;
            if (exp.isAfter(today.plusDays(f.expiresWithinDays()))) return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Reagent get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Reagent not found: " + id));
    }

    public Reagent create(ReagentRequest req) {
        Reagent r = new Reagent();
        applyRequest(r, req);
        Reagent saved = repo.save(r);
        events.save(new ReagentEvent(saved.getId(), saved.getName(),
                ReagentEvent.EventType.CREATED, snapshotJson(saved),
                actor(), null));
        return saved;
    }

    public Reagent update(Long id, ReagentRequest req) {
        Reagent r = get(id);
        if (req.getVersion() != null && !req.getVersion().equals(r.getVersion())) {
            throw new OptimisticLockException(
                    "Reagent was modified by another user. Reload and try again.",
                    null, r);
        }
        Map<String, List<String>> diff = diffAgainstRequest(r, req);
        applyRequest(r, req);
        Reagent saved = repo.save(r);
        if (!diff.isEmpty()) {
            events.save(new ReagentEvent(saved.getId(), saved.getName(),
                    ReagentEvent.EventType.UPDATED, toJson(diff),
                    actor(), null));
        }
        return saved;
    }

    public void delete(Long id) {
        Reagent r = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Reagent not found: " + id));
        String snapshot = snapshotJson(r);
        String name = r.getName();
        repo.delete(r);
        events.save(new ReagentEvent(id, name,
                ReagentEvent.EventType.DELETED, snapshot,
                actor(), null));
    }

    @Transactional(readOnly = true)
    public List<ReagentEvent> history(Long reagentId) {
        return events.findByReagentIdOrderByCreatedAtDesc(reagentId);
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        List<Reagent> all = repo.findAll();
        long total = all.size();
        long expired = 0, low = 0, ok = 0;
        for (Reagent r : all) {
            ReagentStatus s = r.computeStatus();
            switch (s) {
                case EXPIRED -> expired++;
                case LOW_STOCK -> low++;
                case IN_STOCK -> ok++;
            }
        }
        return new DashboardSummary(total, low, expired, ok);
    }

    private void applyRequest(Reagent r, ReagentRequest req) {
        r.setName(req.getName());
        r.setSupplier(req.getSupplier());
        r.setQuantity(req.getQuantity());
        r.setUnit(req.getUnit());
        r.setStorageLocation(req.getStorageLocation());
        r.setExpirationDate(req.getExpirationDate());
        r.setMinimumQuantity(req.getMinimumQuantity() != null
                ? req.getMinimumQuantity()
                : new BigDecimal("5"));
        r.setLotNumber(blankToNull(req.getLotNumber()));
        r.setCasNumber(blankToNull(req.getCasNumber()));
        r.setHazardClass(req.getHazardClass() != null ? req.getHazardClass() : HazardClass.NONE);
    }

    private Map<String, List<String>> diffAgainstRequest(Reagent before, ReagentRequest req) {
        Map<String, List<String>> diff = new LinkedHashMap<>();
        BigDecimal incomingMin = req.getMinimumQuantity() != null
                ? req.getMinimumQuantity()
                : new BigDecimal("5");
        HazardClass incomingHazard = req.getHazardClass() != null
                ? req.getHazardClass()
                : HazardClass.NONE;

        addIfChanged(diff, "name", before.getName(), req.getName());
        addIfChanged(diff, "supplier", before.getSupplier(), req.getSupplier());
        addIfChangedBig(diff, "quantity", before.getQuantity(), req.getQuantity());
        addIfChanged(diff, "unit", before.getUnit(), req.getUnit());
        addIfChanged(diff, "storageLocation", before.getStorageLocation(), req.getStorageLocation());
        addIfChangedDate(diff, "expirationDate", before.getExpirationDate(), req.getExpirationDate());
        addIfChangedBig(diff, "minimumQuantity", before.getMinimumQuantity(), incomingMin);
        addIfChanged(diff, "lotNumber", before.getLotNumber(), blankToNull(req.getLotNumber()));
        addIfChanged(diff, "casNumber", before.getCasNumber(), blankToNull(req.getCasNumber()));
        if (before.getHazardClass() != incomingHazard) {
            diff.put("hazardClass", List.of(str(before.getHazardClass()), str(incomingHazard)));
        }
        return diff;
    }

    private void addIfChanged(Map<String, List<String>> diff, String field, String a, String b) {
        if (!Objects.equals(a, b)) diff.put(field, List.of(str(a), str(b)));
    }

    private void addIfChangedBig(Map<String, List<String>> diff, String field, BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return;
        if (a == null || b == null || a.compareTo(b) != 0) {
            diff.put(field, List.of(str(a), str(b)));
        }
    }

    private void addIfChangedDate(Map<String, List<String>> diff, String field, LocalDate a, LocalDate b) {
        if (!Objects.equals(a, b)) diff.put(field, List.of(str(a), str(b)));
    }

    private String str(Object v) { return v == null ? "" : v.toString(); }

    private String snapshotJson(Reagent r) {
        Map<String, String> snap = new LinkedHashMap<>();
        snap.put("name", str(r.getName()));
        snap.put("supplier", str(r.getSupplier()));
        snap.put("quantity", str(r.getQuantity()));
        snap.put("unit", str(r.getUnit()));
        snap.put("storageLocation", str(r.getStorageLocation()));
        snap.put("expirationDate", str(r.getExpirationDate()));
        snap.put("minimumQuantity", str(r.getMinimumQuantity()));
        snap.put("lotNumber", str(r.getLotNumber()));
        snap.put("casNumber", str(r.getCasNumber()));
        snap.put("hazardClass", str(r.getHazardClass()));
        return toJson(snap);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "system";
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) return "system";
        return name;
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String msg) { super(msg); }
    }
}
