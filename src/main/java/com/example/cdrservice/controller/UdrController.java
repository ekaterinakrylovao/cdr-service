package com.example.cdrservice.controller;

import com.example.cdrservice.service.UdrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/udr")
public class UdrController {

    @Autowired
    private UdrService udrService;

    @GetMapping("/{msisdn}")
    public String getUdrReport(@PathVariable String msisdn, @RequestParam(required = false) String month) {
        return udrService.generateUdrReport(msisdn, month);
    }

    @GetMapping("/all")
    public String getAllUdrReports(@RequestParam String month) {
        return udrService.generateAllUdrReports(month);
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
