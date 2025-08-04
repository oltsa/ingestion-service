package com.oltsa.email.ingestion.controller;

import com.oltsa.email.ingestion.model.IngestionReport;
import com.oltsa.email.ingestion.model.SenderMetrics;
import com.oltsa.email.ingestion.service.EmailIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/")
public class IngestionController {
    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final EmailIngestionService ingestionService;

    public IngestionController(EmailIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("start")
    public ResponseEntity<Void> startIngestion(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ingestionService.startIngestion(file.getInputStream());
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            log.warn("Rejected request to start ingestion: An ingestion process is already running.");
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IOException e) {
            log.error("Failed to get InputStream from multipart file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("status")
    public ResponseEntity<IngestionReport> getStatus() {
        return ResponseEntity.ok(ingestionService.getStatus());
    }

    @GetMapping("top-senders")
    public ResponseEntity<List<SenderMetrics>> getTopSenders() {
        // The spec asks for top 10
        return ResponseEntity.ok(ingestionService.getTopSenders(10));
    }
}