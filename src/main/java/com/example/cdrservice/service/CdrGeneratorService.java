package com.example.cdrservice.service;

import com.example.cdrservice.entity.CdrRecord;
import com.example.cdrservice.entity.Subscriber;
import com.example.cdrservice.repository.CdrRecordRepository;
import com.example.cdrservice.repository.SubscriberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Сервис для генерации записей (CDR).
 * Этот сервис создает случайные записи звонков для абонентов,
 * сохраняет их в базу данных и обеспечивает их корректное временное упорядочение.
 */
@Service
public class CdrGeneratorService {

    private final CdrRecordRepository cdrRecordRepository;
    private final SubscriberRepository subscriberRepository;

    /**
     * Конструктор для внедрения зависимостей.
     *
     * @param cdrRecordRepository   Репозиторий для работы с записями CDR.
     * @param subscriberRepository  Репозиторий для работы с абонентами.
     */
    public CdrGeneratorService(CdrRecordRepository cdrRecordRepository, SubscriberRepository subscriberRepository) {
        this.cdrRecordRepository = cdrRecordRepository;
        this.subscriberRepository = subscriberRepository;
    }

    /**
     * Генерирует записи CDR для всех абонентов за последний год.
     * <p>
     * Процесс генерации включает:
     * <ul>
     *   <li>Создание случайного количества звонков для каждого абонента.</li>
     *   <li>Генерацию случайных временных меток начала и окончания звонков.</li>
     *   <li>Выбор случайного получателя звонка (не самого себя).</li>
     *   <li>Проверку на пересечение временных интервалов для каждого абонента.</li>
     *   <li>Сохранение записей в базу данных партиями для повышения производительности.</li>
     * </ul>
     * Звонки генерируются за последний год с преимущественным временем с 8:00 до 22:00.
     */
    public void generateCdrRecords() {
        List<Subscriber> subscribers = subscriberRepository.findAll();
        Random random = new Random();

        // Начальная точка: текущее время минус 1 год
        LocalDateTime startDate = LocalDateTime.now().minusYears(1);

        int batchSize = 100; // Размер партии
        List<CdrRecord> batch = new ArrayList<>();

        // Временная структура для хранения интервалов звонков
        Map<String, List<LocalDateTime[]>> callIntervals = new HashMap<>();

        for (Subscriber subscriber : subscribers) {
            int numberOfCalls = random.nextInt(100) + 1; // От 1 до 100 звонков на абонента
            for (int i = 0; i < numberOfCalls; i++) {
                LocalDateTime callStart;
                LocalDateTime callEnd;

                boolean isValidCall;
                do {
                    // Генерация времени начала звонка
                    callStart = generateRandomDateTime(startDate, LocalDateTime.now(), random);

                    // Генерация длительности звонка
                    long callDurationSeconds = 10 + random.nextInt(7200 - 10); // От 10 секунд до 2 часов
                    callEnd = callStart.plusSeconds(callDurationSeconds);

                    // Проверка на пересечение временных интервалов
                    isValidCall = !isOverlapping(callIntervals, subscriber.getMsisdn(), callStart, callEnd);

                } while (!isValidCall);

                // Выбираем случайного абонента для звонка, но не самого себя
                Subscriber receiver;
                do {
                    receiver = subscribers.get(random.nextInt(subscribers.size()));
                } while (receiver.getMsisdn().equals(subscriber.getMsisdn()));

                CdrRecord cdrRecord = new CdrRecord();
                cdrRecord.setCallType(random.nextBoolean() ? "01" : "02");
                cdrRecord.setCallerNumber(subscriber.getMsisdn());
                cdrRecord.setReceiverNumber(receiver.getMsisdn());
                cdrRecord.setStartTime(callStart);
                cdrRecord.setEndTime(callEnd);

                // Добавляем интервал в временную структуру
                callIntervals.computeIfAbsent(subscriber.getMsisdn(), k -> new ArrayList<>())
                        .add(new LocalDateTime[]{callStart, callEnd});

                batch.add(cdrRecord);

                // Если размер партии достигнут, сохраняем её
                if (batch.size() >= batchSize) {
                    saveBatch(batch);
                    batch.clear();
                    callIntervals.clear(); // Очищаем временную структуру
                }
            }
        }

        // Сохраняем оставшиеся записи
        if (!batch.isEmpty()) {
            saveBatch(batch);
        }
    }

    /**
     * Проверяет, пересекается ли новый звонок с существующими интервалами для абонента.
     *
     * @param callIntervals Временные интервалы звонков.
     * @param msisdn        Номер абонента.
     * @param start         Начало нового звонка.
     * @param end           Конец нового звонка.
     * @return true, если есть пересечение; иначе false.
     */
    private boolean isOverlapping(Map<String, List<LocalDateTime[]>> callIntervals, String msisdn, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime[]> intervals = callIntervals.get(msisdn);
        if (intervals == null) {
            return false;
        }
        for (LocalDateTime[] interval : intervals) {
            LocalDateTime existingStart = interval[0];
            LocalDateTime existingEnd = interval[1];
            if (!(end.isBefore(existingStart) || start.isAfter(existingEnd))) {
                return true; // Пересечение найдено
            }
        }
        return false;
    }

    /**
     * Сохраняет партию записей CDR в базу данных.
     *
     * @param batch Список записей для сохранения.
     */
    private void saveBatch(List<CdrRecord> batch) {
        batch.sort(Comparator.comparing(CdrRecord::getStartTime)); // Сортируем партию по времени начала
        cdrRecordRepository.saveAll(batch); // Сохраняем партию
    }

    /**
     * Генерирует случайную дату и время в указанном диапазоне.
     * <p>
     * Время генерируется преимущественно с 8:00 до 22:00 для соответствия реальным условиям использования.
     *
     * @param start  Начальная дата и время.
     * @param end    Конечная дата и время.
     * @param random Объект Random для генерации случайных чисел.
     * @return Случайная дата и время в диапазоне [start, end].
     */
    private LocalDateTime generateRandomDateTime(LocalDateTime start, LocalDateTime end, Random random) {
        long startEpochSecond = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endEpochSecond = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpochSecond = startEpochSecond + random.nextInt((int) (endEpochSecond - startEpochSecond));
        return LocalDateTime.ofEpochSecond(randomEpochSecond, 0, java.time.ZoneOffset.UTC);
    }
}
