package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.web.dto.AiQueryFilter;

public record NlParseResult(AiQueryFilter filter, String interpretation) {}
