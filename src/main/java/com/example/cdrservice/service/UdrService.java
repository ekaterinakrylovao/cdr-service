package com.example.cdrservice.service;

import com.example.cdrservice.entity.CdrRecord;
import com.example.cdrservice.repository.CdrRecordRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UdrService {

    private final CdrRecordRepository cdrRecordRepository;

    public UdrService(CdrRecordRepository cdrRecordRepository) {
        this.cdrRecordRepository = cdrRecordRepository;
    }

    public String generateUdrReport(String msisdn, String month) {
        // Нормализация номера
        msisdn = normalizeMsisdn(msisdn);

        // Проверяем, существует ли указанный номер в базе данных
        if (cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)) {
            return "No records found for the specified MSISDN.";
        }

        LocalDateTime start;
        LocalDateTime end;

        if (month != null) {
            // Формируем диапазон дат для указанного месяца
            start = LocalDateTime.parse(month + "-01T00:00:00");
            end = start.plusMonths(1).minusSeconds(1);
        } else {
            // Берем весь тарифицируемый период
            start = cdrRecordRepository.findEarliestStartTime();
            end = cdrRecordRepository.findLatestEndTime();
        }

        List<CdrRecord> records = cdrRecordRepository.findByStartTimeBetween(start, end);

        Map<String, Duration> durations = calculateDurations(records, msisdn);

        return formatUdrReport(
                msisdn,
                durations.get("incoming"),
                durations.get("outcoming")
        );
    }

    public String generateAllUdrReports(String month) {
        // Преобразуем месяц в диапазон дат
        LocalDateTime startOfMonth = LocalDateTime.parse(month + "-01T00:00:00");
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        // Ищем все записи за указанный месяц
        List<CdrRecord> records = cdrRecordRepository.findByStartTimeBetween(startOfMonth, endOfMonth);

        if (records.isEmpty()) {
            return "No records found for the specified period.";
        }

        // Собираем уникальные номера абонентов
        Set<String> allMsisdns = new HashSet<>();
        records.forEach(record -> {
            allMsisdns.add(record.getCallerNumber());
            allMsisdns.add(record.getReceiverNumber());
        });

        StringBuilder result = new StringBuilder();
        for (String msisdn : allMsisdns) {
            // Рассчитываем длительность звонков для каждого абонента
            Map<String, Duration> durations = calculateDurations(records, msisdn);

            result.append(formatUdrReport(
                    msisdn,
                    durations.get("incoming"),
                    durations.get("outcoming")
            ));
            result.append("\n");
        }

        return result.toString();
    }

    public String generateCdrReport(String msisdn, String startDate, String endDate) {
        // Нормализация номера
        msisdn = normalizeMsisdn(msisdn);

        // Проверяем, существует ли указанный номер в базе данных
        if (cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)) {
            throw new RuntimeException("No records found for the specified MSISDN.");
        }

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        // Ищем записи, где абонент был либо caller, либо receiver, и звонок был в указанный период
        List<CdrRecord> records = cdrRecordRepository.findByCallerNumberAndStartTimeBetweenOrReceiverNumberAndStartTimeBetween(
                msisdn, start, end, msisdn, start, end);

        if (records.isEmpty()) {
            throw new RuntimeException("No records found for the specified period.");
        }

        String reportId = UUID.randomUUID().toString();
        String fileName = msisdn + "_" + reportId + ".csv";
        Path filePath = Paths.get("reports", fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (CdrRecord record : records) {
                writer.write(String.format("%s,%s,%s,%s,%s\n",
                        record.getCallType(),
                        record.getCallerNumber(),
                        record.getReceiverNumber(),
                        record.getStartTime(),
                        record.getEndTime()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CDR report", e);
        }

        return reportId;
    }

    private Map<String, Duration> calculateDurations(List<CdrRecord> records, String msisdn) {
        Map<String, Duration> durations = new HashMap<>();
        durations.put("incoming", Duration.ZERO);
        durations.put("outcoming", Duration.ZERO);

        for (CdrRecord record : records) {
            if ("01".equals(record.getCallType())) {
                // Исходящий звонок
                if (record.getCallerNumber().equals(msisdn)) {
                    durations.put("outcoming", durations.get("outcoming").plus(Duration.between(record.getStartTime(), record.getEndTime())));
                }
            } else if ("02".equals(record.getCallType())) {
                // Входящий звонок
                if (record.getReceiverNumber().equals(msisdn)) {
                    durations.put("incoming", durations.get("incoming").plus(Duration.between(record.getStartTime(), record.getEndTime())));
                }
            }
        }

        return durations;
    }

    private String formatUdrReport(String msisdn, Duration incomingDuration, Duration outcomingDuration) {
        return String.format("{\"msisdn\": \"%s\", \"incomingCall\": {\"totalTime\": \"%s\"}, \"outcomingCall\": {\"totalTime\": \"%s\"}}",
                msisdn, formatDuration(incomingDuration), formatDuration(outcomingDuration));
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String normalizeMsisdn(String msisdn) {
        // Убираем лишние символы и приводим к стандартному формату
        return msisdn.replaceAll("[^0-9]", ""); // Оставляем только цифры
    }
}
