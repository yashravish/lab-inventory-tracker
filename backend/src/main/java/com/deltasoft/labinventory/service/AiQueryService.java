package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.config.AiProperties;
import com.deltasoft.labinventory.domain.HazardClass;
import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.domain.ReagentStatus;
import com.deltasoft.labinventory.web.dto.AiQueryFilter;
import com.deltasoft.labinventory.web.dto.AiQueryResponse;
import com.deltasoft.labinventory.web.dto.ReagentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiQueryService {

    private static final Logger log = LoggerFactory.getLogger(AiQueryService.class);
    private static final int DEFAULT_SIZE = 25;
    private static final String DEFAULT_SORT = "expirationDate,asc";

    private final ReagentService reagents;
    private final AiProperties props;
    private final boolean mock;

    public AiQueryService(ReagentService reagents,
                          AiProperties props,
                          @Value("${ai.mock:false}") boolean mock) {
        this.reagents = reagents;
        this.props = props;
        this.mock = mock;
    }

    public AiQueryResponse query(String q, Integer requestedSize) {
        boolean fallback = false;
        NlParseResult parsed;
        if (mock || props.getApiKey() == null || props.getApiKey().isBlank()) {
            parsed = MockNlParser.parse(q);
            if (!mock && (props.getApiKey() == null || props.getApiKey().isBlank())) {
                fallback = true;
            }
        } else {
            try {
                // System prompt + tool schema are sent with cache_control: ephemeral; only the
                // user message varies across requests, so the prefix is a cache hit after the
                // first call.
                AnthropicHttpClient client = new AnthropicHttpClient(props.getApiKey(), props.getModel());
                parsed = client.parse(q);
            } catch (Exception e) {
                log.warn("Anthropic call failed; falling back to MockNlParser: {}", e.getMessage());
                parsed = MockNlParser.parse(q);
                fallback = true;
            }
        }

        AiQueryFilter filter = parsed.filter();
        if (filter.getSort() == null || filter.getSort().isBlank()) {
            filter.setSort(DEFAULT_SORT);
        }
        int size = clampSize(filter.getSize() != null ? filter.getSize()
                : (requestedSize != null ? requestedSize : DEFAULT_SIZE));
        filter.setSize(size);

        SearchFilter search = new SearchFilter(
                filter.getSearch(),
                parseStatus(filter.getStatus()),
                parseHazard(filter.getHazardClass()),
                filter.getExpiresWithinDays());
        PageRequest pageable = PageRequest.of(0, size, parseSort(filter.getSort()));
        Page<Reagent> page = reagents.list(search, pageable);
        Page<ReagentResponse> mapped = page.map(ReagentResponse::from);

        return new AiQueryResponse(parsed.interpretation(), filter, mapped, fallback ? Boolean.TRUE : null);
    }

    private static int clampSize(int s) {
        if (s < 1) return 1;
        if (s > 200) return 200;
        return s;
    }

    private static ReagentStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return ReagentStatus.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    private static HazardClass parseHazard(String s) {
        if (s == null || s.isBlank()) return null;
        try { return HazardClass.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    private static Sort parseSort(String spec) {
        if (spec == null || spec.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "expirationDate");
        }
        String[] parts = spec.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        List<String> allowedFields = List.of(
                "name", "supplier", "quantity", "storageLocation", "expirationDate");
        if (!allowedFields.contains(field)) {
            return Sort.by(Sort.Direction.ASC, "expirationDate");
        }
        return Sort.by(dir, field);
    }
}
