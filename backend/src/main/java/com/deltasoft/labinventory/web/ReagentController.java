package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.ReagentStatus;
import com.deltasoft.labinventory.service.ReagentService;
import com.deltasoft.labinventory.service.SearchFilter;
import com.deltasoft.labinventory.web.dto.ReagentEventResponse;
import com.deltasoft.labinventory.web.dto.ReagentRequest;
import com.deltasoft.labinventory.web.dto.ReagentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/reagents")
public class ReagentController {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "name", "supplier", "quantity", "storageLocation", "expirationDate"
    );

    private final ReagentService service;

    public ReagentController(ReagentService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ReagentResponse> list(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "hazardClass", required = false) String hazardClass,
            @RequestParam(value = "expiresWithinDays", required = false) Integer expiresWithinDays,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        validateSort(pageable.getSort());
        SearchFilter filter = new SearchFilter(
                search,
                parseStatus(status),
                parseHazardClass(hazardClass),
                validateDays(expiresWithinDays));
        return service.list(filter, pageable).map(ReagentResponse::from);
    }

    private static ReagentStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ReagentStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported status: " + raw + ". Allowed: IN_STOCK, LOW_STOCK, EXPIRED");
        }
    }

    private static HazardClass parseHazardClass(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return HazardClass.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported hazardClass: " + raw + ". Allowed: FLAMMABLE, CORROSIVE, TOXIC, NONE");
        }
    }

    private static Integer validateDays(Integer days) {
        if (days == null) return null;
        if (days < 0) {
            throw new IllegalArgumentException("expiresWithinDays must be >= 0");
        }
        return days;
    }

    @GetMapping("/{id}")
    public ReagentResponse get(@PathVariable Long id) {
        return ReagentResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ReagentResponse> create(@Valid @RequestBody ReagentRequest req) {
        var saved = service.create(req);
        return ResponseEntity.created(URI.create("/api/reagents/" + saved.getId()))
                .body(ReagentResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ReagentResponse update(@PathVariable Long id, @Valid @RequestBody ReagentRequest req) {
        return ReagentResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public List<ReagentEventResponse> history(@PathVariable Long id) {
        return service.history(id).stream().map(ReagentEventResponse::from).toList();
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Unsupported sort property: " + order.getProperty()
                                + ". Allowed: " + SORTABLE_FIELDS);
            }
        }
    }
}
