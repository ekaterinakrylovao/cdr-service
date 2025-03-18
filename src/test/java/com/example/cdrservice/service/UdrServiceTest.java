package com.example.cdrservice.service;

import com.example.cdrservice.entity.CdrRecord;
import com.example.cdrservice.repository.CdrRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UdrServiceTest {

    @Mock
    private CdrRecordRepository cdrRecordRepository;

    @InjectMocks
    private UdrService udrService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Проверяет корректность формирования UDR-отчёта для абонента с учётом как входящих,
     * так и исходящих звонков за указанный месяц.
     */
    @Test
    void testGenerateUdrReport() {
        CdrRecord record1 = new CdrRecord();
        record1.setCallType("01");
        record1.setCallerNumber("79991112233");
        record1.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record1.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        CdrRecord record2 = new CdrRecord();
        record2.setCallType("02");
        record2.setReceiverNumber("79991112233");
        record2.setStartTime(LocalDateTime.of(2024, 3, 1, 11, 0));
        record2.setEndTime(LocalDateTime.of(2024, 3, 1, 11, 10));

        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                "79991112233",
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(Arrays.asList(record1, record2));

        String result = udrService.generateUdrReport("79991112233", "2024-03");

        assertThat(result).contains("\"msisdn\": \"79991112233\"");
        assertThat(result).contains("\"totalTime\": \"00:05:00\""); // Исходящие
        assertThat(result).contains("\"totalTime\": \"00:10:00\""); // Входящие
    }

    /**
     * Проверяет поведение сервиса, если для указанного номера нет записей в базе данных.
     */
    @Test
    void testGenerateUdrReport_NoRecords() {
        when(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber("79991112233")).thenReturn(true);

        String result = udrService.generateUdrReport("79991112233", "2024-03");

        assertThat(result).isEqualTo("No records found for the specified MSISDN.");
    }

    /**
     * Проверяет формирование UDR-отчёта для случая, когда у абонента есть только входящие звонки.
     */
    @Test
    void testGenerateUdrReport_OnlyIncomingCalls() {
        CdrRecord record = new CdrRecord();
        record.setCallType("02");
        record.setReceiverNumber("79991112233");
        record.setStartTime(LocalDateTime.of(2024, 3, 1, 11, 0));
        record.setEndTime(LocalDateTime.of(2024, 3, 1, 11, 10));

        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                "79991112233",
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(List.of(record));

        String result = udrService.generateUdrReport("79991112233", "2024-03");

        assertThat(result).contains("\"msisdn\": \"79991112233\"");
        assertThat(result).contains("\"totalTime\": \"00:10:00\""); // Входящие
        assertThat(result).contains("\"totalTime\": \"00:00:00\""); // Исходящие
    }

    /**
     * Проверяет формирование UDR-отчёта для случая, когда у абонента есть только исходящие звонки.
     */
    @Test
    void testGenerateUdrReport_OnlyOutgoingCalls() {
        CdrRecord record = new CdrRecord();
        record.setCallType("01");
        record.setCallerNumber("79991112233");
        record.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                "79991112233",
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(List.of(record));

        String result = udrService.generateUdrReport("79991112233", "2024-03");

        assertThat(result).contains("\"msisdn\": \"79991112233\"");
        assertThat(result).contains("\"totalTime\": \"00:05:00\""); // Исходящие
        assertThat(result).contains("\"totalTime\": \"00:00:00\""); // Входящие
    }

    /**
     * Проверяет поведение сервиса, если за указанный период нет записей для всех абонентов.
     */
    @Test
    void testGenerateAllUdrReports_NoRecords() {
        when(cdrRecordRepository.findByStartTimeBetween(
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(List.of());

        String result = udrService.generateAllUdrReports("2024-03");

        assertThat(result).isEqualTo("No records found for the specified period.");
    }

    /**
     * Проверяет формирование UDR-отчётов для нескольких абонентов за указанный период.
     */
    @Test
    void testGenerateAllUdrReports_MultipleSubscribers() {
        CdrRecord record1 = new CdrRecord();
        record1.setCallType("01");
        record1.setCallerNumber("79991112233");
        record1.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record1.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        CdrRecord record2 = new CdrRecord();
        record2.setCallType("02");
        record2.setReceiverNumber("79992221122");
        record2.setStartTime(LocalDateTime.of(2024, 3, 1, 11, 0));
        record2.setEndTime(LocalDateTime.of(2024, 3, 1, 11, 10));

        when(cdrRecordRepository.findByStartTimeBetween(
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(Arrays.asList(record1, record2));

        String result = udrService.generateAllUdrReports("2024-03");

        assertThat(result).contains("\"msisdn\": \"79991112233\"");
        assertThat(result).contains("\"totalTime\": \"00:05:00\""); // Исходящие для 79991112233
        assertThat(result).contains("\"totalTime\": \"00:00:00\""); // Входящие для 79991112233

        assertThat(result).contains("\"msisdn\": \"79992221122\"");
        assertThat(result).contains("\"totalTime\": \"00:10:00\""); // Входящие для 79992221122
        assertThat(result).contains("\"totalTime\": \"00:00:00\""); // Исходящие для 79992221122
    }

    /**
     * Проверяет поведение сервиса, если за указанный период нет записей для генерации CDR-отчёта.
     */
    @Test
    void testGenerateCdrReport_NoRecords() {
        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                "79991112233",
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(List.of());

        try {
            udrService.generateCdrReport("79991112233", "2024-03-01T00:00:00", "2024-03-31T23:59:59");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("No records found for the specified period.");
        }
    }

    /**
     * Проверяет генерацию CDR-отчёта для абонента с несколькими записями за указанный период.
     */
    @Test
    void testGenerateCdrReport_MultipleRecords() {
        CdrRecord record1 = new CdrRecord();
        record1.setCallType("01");
        record1.setCallerNumber("79991112233");
        record1.setReceiverNumber("79992221122");
        record1.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record1.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        CdrRecord record2 = new CdrRecord();
        record2.setCallType("02");
        record2.setCallerNumber("79993334455");
        record2.setReceiverNumber("79991112233");
        record2.setStartTime(LocalDateTime.of(2024, 3, 1, 11, 0));
        record2.setEndTime(LocalDateTime.of(2024, 3, 1, 11, 10));

        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                "79991112233",
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(Arrays.asList(record1, record2));

        String reportId = udrService.generateCdrReport("79991112233", "2024-03-01T00:00:00", "2024-03-31T23:59:59");

        assertThat(reportId).isNotNull();
        assertThat(reportId.length()).isEqualTo(36); // UUID имеет длину 36 символов
    }

    /**
     * Косвенно проверяет метод нормализации номера телефона.
     */
    @Test
    void testNormalizeMsisdn_ThroughGenerateUdrReport() {
        String msisdn = "+7 (999) 111-22-33";
        String month = "2024-03";

        CdrRecord record = new CdrRecord();
        record.setCallType("01");
        record.setCallerNumber("79991112233");
        record.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                "79991112233",
                LocalDateTime.of(2024, 3, 1, 0, 0),
                LocalDateTime.of(2024, 3, 31, 23, 59, 59)))
                .thenReturn(List.of(record));

        String result = udrService.generateUdrReport(msisdn, month);

        assertThat(result).contains("\"msisdn\": \"79991112233\"");
    }

    /**
     * Использование findEarliestStartTime и findLatestEndTime.
     */
    @Test
    void testGenerateUdrReport_WithoutMonth() {
        String msisdn = "79991112233";

        CdrRecord record1 = new CdrRecord();
        record1.setCallType("01");
        record1.setCallerNumber(msisdn);
        record1.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record1.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        when(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)).thenReturn(false);
        when(cdrRecordRepository.findEarliestStartTime()).thenReturn(LocalDateTime.of(2024, 3, 1, 10, 0));
        when(cdrRecordRepository.findLatestEndTime()).thenReturn(LocalDateTime.of(2024, 3, 1, 10, 5));
        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                msisdn,
                LocalDateTime.of(2024, 3, 1, 10, 0),
                LocalDateTime.of(2024, 3, 1, 10, 5)))
                .thenReturn(List.of(record1));

        String result = udrService.generateUdrReport(msisdn, null);

        assertThat(result).contains("\"msisdn\": \"79991112233\"");
        assertThat(result).contains("\"totalTime\": \"00:05:00\""); // Исходящие
        assertThat(result).contains("\"totalTime\": \"00:00:00\""); // Входящие

        verify(cdrRecordRepository, times(1)).findEarliestStartTime();
        verify(cdrRecordRepository, times(1)).findLatestEndTime();
    }

    /**
     * Записи с указанным msisdn существуют.
     */
    @Test
    void testDoesNotExistByCallerNumberOrReceiverNumber_RecordExists() {
        String msisdn = "79991112233";

        when(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)).thenReturn(false);

        boolean result = cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn);

        assertThat(result).isFalse();
        verify(cdrRecordRepository, times(1)).doesNotExistByCallerNumberOrReceiverNumber(msisdn);
    }

    @Test
    void testGenerateUdrReport_NoRecordsFound() {
        String msisdn = "79991112233";
        String month = "2024-03";

        // Мокируем репозиторий, чтобы вернуть пустой список записей
        when(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)).thenReturn(true);

        String result = udrService.generateUdrReport(msisdn, month);

        assertThat(result).isEqualTo("No records found for the specified MSISDN.");
    }

    @Test
    void testGenerateCdrReport_NoRecordsFound() {
        String msisdn = "79991112233";
        String startDate = "2024-03-01T00:00:00";
        String endDate = "2024-03-31T23:59:59";

        // Мокируем репозиторий, чтобы вернуть пустой список записей
        when(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)).thenReturn(true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            udrService.generateCdrReport(msisdn, startDate, endDate);
        });

        assertThat(exception.getMessage()).isEqualTo("No records found for the specified MSISDN.");
    }

    @Test
    void testGenerateAllUdrReports_NoRecordsFound() {
        String month = "2024-03";

        // Мокируем репозиторий, чтобы вернуть пустой список записей
        LocalDateTime startOfMonth = LocalDateTime.parse(month + "-01T00:00:00");
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        when(cdrRecordRepository.findByStartTimeBetween(startOfMonth, endOfMonth)).thenReturn(List.of());

        String result = udrService.generateAllUdrReports(month);

        assertThat(result).isEqualTo("No records found for the specified period.");
    }

    @Test
    void testGenerateUdrReport_NoRecordsInPeriod() {
        String msisdn = "79991112233";
        String month = "2024-03";

        // Мокируем репозиторий, чтобы вернуть пустой список записей
        when(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber(msisdn)).thenReturn(false);
        when(cdrRecordRepository.findRecordsForMsisdnInPeriod(
                eq(msisdn),
                eq(LocalDateTime.parse("2024-03-01T00:00:00")),
                eq(LocalDateTime.parse("2024-03-31T23:59:59"))
        )).thenReturn(List.of());

        String result = udrService.generateUdrReport(msisdn, month);

        assertThat(result).isEqualTo("No records found for the specified MSISDN.");
    }
}
