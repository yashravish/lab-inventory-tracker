package com.deltasoft.labinventory.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiQueryResponse {

    private String interpretation;
    private AiQueryFilter filter;
    private Page<ReagentResponse> page;
    private Boolean fallback;

    public AiQueryResponse() {}

    public AiQueryResponse(String interpretation, AiQueryFilter filter,
                           Page<ReagentResponse> page, Boolean fallback) {
        this.interpretation = interpretation;
        this.filter = filter;
        this.page = page;
        this.fallback = fallback;
    }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    public AiQueryFilter getFilter() { return filter; }
    public void setFilter(AiQueryFilter filter) { this.filter = filter; }
    public Page<ReagentResponse> getPage() { return page; }
    public void setPage(Page<ReagentResponse> page) { this.page = page; }
    public Boolean getFallback() { return fallback; }
    public void setFallback(Boolean fallback) { this.fallback = fallback; }
}
