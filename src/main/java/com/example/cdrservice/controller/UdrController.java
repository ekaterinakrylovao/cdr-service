package com.example.cdrservice.controller;

import com.example.cdrservice.service.UdrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/udr")
public class UdrController {

    private final UdrService udrService;

    public UdrController(UdrService udrService) {
        this.udrService = udrService;
    }

    @GetMapping("/{msisdn}")
    public ResponseEntity<String> getUdrReport(@PathVariable String msisdn, @RequestParam(required = false) String month) {
        String report = udrService.generateUdrReport(msisdn, month);
        if (report.contains("No records found")) {
            return ResponseEntity.status(404).body(report);
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/all")
    public ResponseEntity<String> getAllUdrReports(@RequestParam String month) {
        String reports = udrService.generateAllUdrReports(month);
        if (reports.contains("No records found")) {
            return ResponseEntity.status(404).body(reports);
        }
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/cdr-report/{msisdn}")
    public ResponseEntity<String> generateCdrReport(@PathVariable String msisdn, @RequestParam String startDate, @RequestParam String endDate) {
        try {
            String reportId = udrService.generateCdrReport(msisdn, startDate, endDate);
            return ResponseEntity.ok("Report generated with ID: " + reportId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
