package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.service.AiQueryService;
import com.deltasoft.labinventory.web.dto.AiQueryRequest;
import com.deltasoft.labinventory.web.dto.AiQueryResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiQueryController {

    private final AiQueryService service;

    public AiQueryController(AiQueryService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public AiQueryResponse query(@RequestBody AiQueryRequest req) {
        String q = req == null ? "" : req.getQ();
        if (q == null) q = "";
        Integer size = req == null ? null : req.getSize();
        return service.query(q, size);
    }
}
