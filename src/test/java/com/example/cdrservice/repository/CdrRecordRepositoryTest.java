package com.example.cdrservice.repository;

import com.example.cdrservice.entity.CdrRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CdrRecordRepositoryTest {

    @Autowired
    private CdrRecordRepository cdrRecordRepository;

    @BeforeEach
    void setUp() {
        // Создаём записи для тестов
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

        cdrRecordRepository.saveAll(List.of(record1, record2));
    }

    /**
     * Описание: Проверяет метод проверки существования номера.
     * Сценарий:
     * - Для существующего номера метод возвращает false.
     * - Для несуществующего номера метод возвращает true.
     */
    @Test
    void testDoesNotExistByCallerNumberOrReceiverNumber() {
        assertThat(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber("79991112233")).isFalse();
        assertThat(cdrRecordRepository.doesNotExistByCallerNumberOrReceiverNumber("79993334455")).isTrue();
    }

    /**
     * Описание: Проверяет метод поиска самой ранней даты начала звонка.
     * Сценарий:
     * - В базе есть записи с разными временными метками.
     * - Метод должен вернуть самую раннюю дату.
     */
    @Test
    void testFindEarliestStartTime() {
        LocalDateTime earliestTime = cdrRecordRepository.findEarliestStartTime();
        assertThat(earliestTime).isEqualTo(LocalDateTime.of(2024, 3, 1, 10, 0));
    }

    /**
     * Описание: Проверяет метод поиска самой поздней даты окончания звонка.
     * Сценарий:
     * - В базе есть записи с разными временными метками.
     * - Метод должен вернуть самую позднюю дату.
     */
    @Test
    void testFindLatestEndTime() {
        LocalDateTime latestTime = cdrRecordRepository.findLatestEndTime();
        assertThat(latestTime).isEqualTo(LocalDateTime.of(2024, 3, 1, 11, 10));
    }

    /**
     * Описание: Проверяет метод поиска записей в заданном временном диапазоне.
     * Сценарий:
     * - Задаётся диапазон времени.
     * - Метод должен вернуть только те записи, которые попадают в этот диапазон.
     */
    @Test
    void testFindByStartTimeBetween() {
        List<CdrRecord> records = cdrRecordRepository.findByStartTimeBetween(
                LocalDateTime.of(2024, 3, 1, 10, 0),
                LocalDateTime.of(2024, 3, 1, 11, 0)
        );
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getStartTime()).isEqualTo(LocalDateTime.of(2024, 3, 1, 10, 0));
        assertThat(records.get(1).getStartTime()).isEqualTo(LocalDateTime.of(2024, 3, 1, 11, 0));
    }

    /**
     * Проверяет сохранение и извлечение записи из базы данных.
     * Сценарий:
     * - Создаётся новая запись CDR.
     * - Сохраняется в базу данных.
     * - Извлекается из базы данных и проверяется её корректность.
     */
    @Test
    void testSaveAndRetrieveRecord() {
        CdrRecord record = new CdrRecord();
        record.setCallType("01");
        record.setCallerNumber("79991112233");
        record.setReceiverNumber("79992221122");
        record.setStartTime(LocalDateTime.of(2024, 3, 1, 10, 0));
        record.setEndTime(LocalDateTime.of(2024, 3, 1, 10, 5));

        cdrRecordRepository.save(record);

        CdrRecord retrievedRecord = cdrRecordRepository.findById(record.getId()).orElse(null);

        assertThat(retrievedRecord).isNotNull();
        assertThat(retrievedRecord.getCallerNumber()).isEqualTo("79991112233");
        assertThat(retrievedRecord.getReceiverNumber()).isEqualTo("79992221122");
        assertThat(retrievedRecord.getStartTime()).isEqualTo(LocalDateTime.of(2024, 3, 1, 10, 0));
        assertThat(retrievedRecord.getEndTime()).isEqualTo(LocalDateTime.of(2024, 3, 1, 10, 5));
    }
}
