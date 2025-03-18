package com.example.cdrservice.controller;

import com.example.cdrservice.service.UdrService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    public ResponseEntity<String> getUdrReport(@PathVariable String msisdn,
                                               @RequestParam(required = false) String month) {
        if (month != null) {
            ResponseEntity<String> dateValidation = validateMonthFormat(month);
            if (dateValidation != null) {
                return dateValidation;
            }
        }

        String report = udrService.generateUdrReport(msisdn, month);
        if (report.contains("No records found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(report);
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
        ResponseEntity<String> dateValidation = validateMonthFormat(month);
        if (dateValidation != null) {
            return dateValidation;
        }

        String reports = udrService.generateAllUdrReports(month);
        if (reports.contains("No records found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(reports);
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
    public ResponseEntity<String> generateCdrReport(@PathVariable String msisdn,
                                                    @RequestParam String startDate,
                                                    @RequestParam String endDate) {
        ResponseEntity<String> dateValidation = validateDateTimeRange(startDate, endDate);
        if (dateValidation != null) {
            return dateValidation;
        }

        try {
            String reportId = udrService.generateCdrReport(msisdn, startDate, endDate);
            return ResponseEntity.ok("Report generated with ID: " + reportId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Проверяет формат месяца (YYYY-MM).
     *
     * @param month Месяц в формате "YYYY-MM".
     * @return ResponseEntity с ошибкой, если формат неверный, или null, если формат корректен.
     */
    private ResponseEntity<String> validateMonthFormat(String month) {
        try {
            LocalDateTime.parse(month + "-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid month format. Expected format: YYYY-MM");
        }
        return null;
    }

    /**
     * Проверяет формат даты и время (YYYY-MM-DDTHH:mm:ss), а также корректность диапазона.
     *
     * @param startDate Начальная дата.
     * @param endDate   Конечная дата.
     * @return ResponseEntity с ошибкой, если формат неверный или диапазон некорректен, или null, если всё в порядке.
     */
    private ResponseEntity<String> validateDateTimeRange(String startDate, String endDate) {
        LocalDateTime start;
        LocalDateTime end;

        try {
            start = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid startDate format. Expected format: YYYY-MM-DDTHH:mm:ss");
        }

        try {
            end = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid endDate format. Expected format: YYYY-MM-DDTHH:mm:ss");
        }

        if (start.isAfter(end)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("startDate must be before endDate");
        }

        return null;
    }
}
