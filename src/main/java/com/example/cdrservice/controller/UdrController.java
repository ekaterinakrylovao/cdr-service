package com.example.cdrservice.controller;

import com.example.cdrservice.service.UdrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с отчётами по CDR-записям.
 * Предоставляет эндпоинты для:
 * <ul>
 *   <li>Получения индивидуальных отчётов для конкретных абонентов.</li>
 *   <li>Получения консолидированных отчётов для всех абонентов за указанный период.</li>
 *   <li>Генерации CDR-отчётов в формате CSV.</li>
 * </ul>
 */
@RestController
@RequestMapping("/udr")
public class UdrController {

    private final UdrService udrService;

    public UdrController(UdrService udrService) {
        this.udrService = udrService;
    }

    /**
     * Получает UDR для указанного абонента.
     *
     * @param msisdn Номер абонента (MSISDN).
     * @param month  Месяц в формате "YYYY-MM" (опционально). Если не указан, используется весь период.
     * @return ResponseEntity с JSON-отчетом или сообщением об ошибке (HTTP 404, если записи отсутствуют).
     */
    @GetMapping("/{msisdn}")
    public ResponseEntity<String> getUdrReport(@PathVariable String msisdn, @RequestParam(required = false) String month) {
        String report = udrService.generateUdrReport(msisdn, month);
        if (report.contains("No records found")) {
            return ResponseEntity.status(404).body(report);
        }
        return ResponseEntity.ok(report);
    }

    /**
     * Получает консолидированные отчёты для всех абонентов за указанный месяц.
     *
     * @param month Месяц в формате "YYYY-MM".
     * @return ResponseEntity с JSON-отчетами или сообщением об ошибке (HTTP 404, если записи отсутствуют).
     */
    @GetMapping("/all")
    public ResponseEntity<String> getAllUdrReports(@RequestParam String month) {
        String reports = udrService.generateAllUdrReports(month);
        if (reports.contains("No records found")) {
            return ResponseEntity.status(404).body(reports);
        }
        return ResponseEntity.ok(reports);
    }

    /**
     * Генерирует CDR-отчёт в формате CSV для указанного абонента за указанный период.
     *
     * @param msisdn    Номер абонента (MSISDN).
     * @param startDate Начальная дата и время периода в формате "YYYY-MM-DDTHH:mm:ss".
     * @param endDate   Конечная дата и время периода в формате "YYYY-MM-DDTHH:mm:ss".
     * @return ResponseEntity с уникальным идентификатором отчёта или сообщением об ошибке (HTTP 404, если записи отсутствуют).
     */
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
