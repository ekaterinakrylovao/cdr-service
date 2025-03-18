package com.example.cdrservice.service;

import com.example.cdrservice.entity.CdrRecord;
import com.example.cdrservice.entity.Subscriber;
import com.example.cdrservice.repository.CdrRecordRepository;
import com.example.cdrservice.repository.SubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CdrGeneratorServiceTest {

    @Autowired
    private CdrGeneratorService cdrGeneratorService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private CdrRecordRepository cdrRecordRepository;

    @BeforeEach
    void setUp() {
        // Очищаем базу перед каждым тестом
        cdrRecordRepository.deleteAll();
        subscriberRepository.deleteAll();

        // Создаем тестовых абонентов
        Subscriber subscriber1 = new Subscriber();
        subscriber1.setMsisdn("79991112233");
        subscriberRepository.save(subscriber1);

        Subscriber subscriber2 = new Subscriber();
        subscriber2.setMsisdn("79992221122");
        subscriberRepository.save(subscriber2);
    }

    /**
     * Проверяет, что метод генерирует корректное количество записей.
     */
    @Test
    void testGenerateCdrRecords_Count() {
        cdrGeneratorService.generateCdrRecords();

        List<CdrRecord> records = cdrRecordRepository.findAll();
        assertThat(records).isNotEmpty();
        assertThat(records.size()).isGreaterThan(0); // Хотя бы одна запись должна быть
    }

    /**
     * Проверяет, что временные метки находятся в допустимом диапазоне.
     */
    @Test
    void testGenerateCdrRecords_TimeRange() {
        cdrGeneratorService.generateCdrRecords();

        List<CdrRecord> records = cdrRecordRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yearAgo = now.minusYears(1);

        for (CdrRecord record : records) {
            assertThat(record.getStartTime()).isAfter(yearAgo);
            assertThat(record.getStartTime()).isBefore(now);
            assertThat(record.getEndTime()).isAfter(record.getStartTime());
        }
    }

    /**
     * Проверяет, что длительность звонков находится в пределах от 10 секунд до 2 часов.
     */
    @Test
    void testGenerateCdrRecords_CallDuration() {
        cdrGeneratorService.generateCdrRecords();

        List<CdrRecord> records = cdrRecordRepository.findAll();
        for (CdrRecord record : records) {
            Duration duration = Duration.between(record.getStartTime(), record.getEndTime());
            assertThat(duration.getSeconds()).isGreaterThanOrEqualTo(10);
            assertThat(duration.getSeconds()).isLessThanOrEqualTo(7200); // 2 часа = 7200 секунд
        }
    }

    /**
     * Проверяет, что абоненты не совершают звонков сами себе.
     */
    @Test
    void testGenerateCdrRecords_NoSelfCalls() {
        cdrGeneratorService.generateCdrRecords();

        List<CdrRecord> records = cdrRecordRepository.findAll();
        for (CdrRecord record : records) {
            assertThat(record.getCallerNumber()).isNotEqualTo(record.getReceiverNumber());
        }
    }

    /**
     * Проверяет, что записи сохраняются в базу данных.
     */
    @Test
    void testGenerateCdrRecords_RecordsSaved() {
        cdrGeneratorService.generateCdrRecords();

        List<CdrRecord> records = cdrRecordRepository.findAll();
        assertThat(records).isNotEmpty();
    }
}
