package com.deltasoft.labinventory.web;

import com.deltasoft.labinventory.service.ReagentCsvService;
import com.deltasoft.labinventory.service.ReagentCsvService.ImportResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reagents")
public class ReagentCsvController {

    private final ReagentCsvService csvService;

    public ReagentCsvController(ReagentCsvService csvService) {
        this.csvService = csvService;
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public void export(HttpServletResponse response) throws IOException {
        String filename = "reagents-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (PrintWriter writer = response.getWriter()) {
            csvService.exportTo(writer);
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@RequestPart("file") MultipartFile file) throws IOException {
        return csvService.importFrom(file.getInputStream());
    }
}
