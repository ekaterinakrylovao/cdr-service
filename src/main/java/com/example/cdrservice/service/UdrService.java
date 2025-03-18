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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сервис для генерации отчётов по CDR-записям.
 * Этот сервис предоставляет функциональность для создания:
 * <ul>
 *   <li>Индивидуальных отчётов для конкретных абонентов (UDR).</li>
 *   <li>Консолидированных отчётов для всех абонентов за указанный период (UDR).</li>
 *   <li>CDR-отчётов в формате CSV.</li>
 * </ul>
 */
@Service
public class UdrService {

    private final CdrRecordRepository cdrRecordRepository;

    public UdrService(CdrRecordRepository cdrRecordRepository) {
        this.cdrRecordRepository = cdrRecordRepository;
    }

    /**
     * Генерирует UDR для указанного абонента.
     * <p>
     * Отчёт включает информацию о входящих и исходящих звонках за указанный месяц или весь доступный период.
     *
     * @param msisdn Номер абонента (MSISDN).
     * @param month  Месяц в формате "YYYY-MM" (опционально). Если не указан, используется весь период.
     * @return JSON-строка с данными общей длительности входящих и исходящих звонков.
     *         Если записи отсутствуют, возвращается сообщение "No records found for the specified MSISDN."
     */
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

        List<CdrRecord> records = cdrRecordRepository.findRecordsForMsisdnInPeriod(msisdn, start, end);
        if (records.isEmpty()) {
            return "No records found for the specified MSISDN.";
        }

        Map<String, Duration> durations = calculateDurations(records, msisdn);

        return formatUdrReport(
                msisdn,
                durations.get("incoming"),
                durations.get("outcoming")
        );
    }

    /**
     * Генерирует консолидированные отчёты для всех абонентов за указанный месяц.
     * <p>
     * Для каждого абонента, участвовавшего в звонках за указанный период, создается UDR.
     *
     * @param month Месяц в формате "YYYY-MM".
     * @return Строка с JSON-объектами, разделенными символом новой строки (\n).
     *         Если записи отсутствуют, возвращается сообщение "No records found for the specified period."
     */
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
        Set<String> allMsisdns = records.parallelStream()
                .flatMap(record -> Stream.of(record.getCallerNumber(), record.getReceiverNumber()))
                .collect(Collectors.toSet());

        StringBuilder result = new StringBuilder();
        allMsisdns.parallelStream().forEach(msisdn -> {
            // Рассчитываем длительность звонков для каждого абонента
            Map<String, Duration> durations = calculateDurations(records, msisdn);
            String report = formatUdrReport(
                    msisdn,
                    durations.get("incoming"),
                    durations.get("outcoming")
            );
            synchronized (result) {
                result.append(report).append("\n");
            }
        });

        return result.toString();
    }

    /**
     * Генерирует CDR-отчёт в формате CSV для указанного абонента за указанный период.
     * <p>
     * Отчёт сохраняется в файл в директории "reports" с именем, содержащим MSISDN и уникальный идентификатор.
     *
     * @param msisdn    Номер абонента (MSISDN).
     * @param startDate Начальная дата и время периода в формате "YYYY-MM-DDTHH:mm:ss".
     * @param endDate   Конечная дата и время периода в формате "YYYY-MM-DDTHH:mm:ss".
     * @return Уникальный идентификатор отчёта (UUID).
     * @throws RuntimeException Если записи отсутствуют или возникла ошибка при записи файла.
     */
    public String generateCdrReport(String msisdn, String startDate, String endDate) {
        // Нормализация номера
        msisdn = normalizeMsisdn(msisdn);

        // Проверяем, существует ли указанный номер в базе данных
        if (cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)) {
            throw new RuntimeException("No records found for the specified MSISDN.");
        }

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        List<CdrRecord> records = cdrRecordRepository.findRecordsForMsisdnInPeriod(msisdn, start, end);
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

    /**
     * Рассчитывает общую длительность входящих и исходящих звонков для указанного абонента.
     *
     * @param records Список CDR-записей.
     * @param msisdn  Номер абонента (MSISDN).
     * @return Карта с длительностями звонков ("incoming" и "outcoming").
     */
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

    /**
     * Форматирует данные отчёта в JSON-строку.
     *
     * @param msisdn            Номер абонента (MSISDN).
     * @param incomingDuration  Длительность входящих звонков.
     * @param outcomingDuration Длительность исходящих звонков.
     * @return JSON-строка с данными отчёта.
     */
    private String formatUdrReport(String msisdn, Duration incomingDuration, Duration outcomingDuration) {
        return String.format("{\"msisdn\": \"%s\", \"incomingCall\": {\"totalTime\": \"%s\"}, \"outcomingCall\": {\"totalTime\": \"%s\"}}",
                msisdn, formatDuration(incomingDuration), formatDuration(outcomingDuration));
    }

    /**
     * Форматирует длительность в строку в формате "HH:mm:ss".
     *
     * @param duration Длительность.
     * @return Строка в формате "HH:mm:ss".
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Нормализует номер абонента (MSISDN), удаляя все символы, кроме цифр.
     *
     * @param msisdn Номер абонента (MSISDN).
     * @return Нормализованный номер.
     */
    private String normalizeMsisdn(String msisdn) {
        // Убираем лишние символы и приводим к стандартному формату
        return msisdn.replaceAll("[^0-9]", ""); // Оставляем только цифры
    }
}
