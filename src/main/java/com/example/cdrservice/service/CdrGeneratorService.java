package com.example.cdrservice.service;

import com.example.cdrservice.entity.CdrRecord;
import com.example.cdrservice.entity.Subscriber;
import com.example.cdrservice.repository.CdrRecordRepository;
import com.example.cdrservice.repository.SubscriberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
     * Генерирует записи CDR для всех абонентов.
     * <p>
     * Процесс генерации включает:
     * <ul>
     *   <li>Создание случайного количества звонков для каждого абонента.</li>
     *   <li>Генерацию случайных временных меток начала и окончания звонков.</li>
     *   <li>Выбор случайного получателя звонка (не самого себя).</li>
     *   <li>Сохранение всех записей в базу данных после их сортировки по времени начала.</li>
     * </ul>
     * Звонки генерируются за последний год с преимущественным временем с 8:00 до 22:00.
     */
    public void generateCdrRecords() {
        List<Subscriber> subscribers = subscriberRepository.findAll();
        Random random = new Random();

        // Начальная точка: текущее время минус 1 год
        LocalDateTime startDate = LocalDateTime.now().minusYears(1);

        List<CdrRecord> allCdrRecords = new ArrayList<>();

        for (Subscriber subscriber : subscribers) {
            int numberOfCalls = random.nextInt(100) + 1; // От 1 до 100 звонков на абонента
            for (int i = 0; i < numberOfCalls; i++) {
                // Генерация времени начала звонка (преимущественно с 8:00 до 22:00)
                int hour = 8 + random.nextInt(14); // Случайный час от 8 до 21
                int minute = random.nextInt(60);
                int second = random.nextInt(60);

                LocalDateTime callStart = startDate.plusDays(random.nextInt(365))
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(second);

                // Генерация длительности звонка: от 10 секунд до 2 часов
                long callDurationSeconds = 10 + random.nextInt(7200 - 10); // От 10 секунд до 2 часов (7200 секунд)
                LocalDateTime callEnd = callStart.plusSeconds(callDurationSeconds);

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

                allCdrRecords.add(cdrRecord);
            }
        }

        // Сортируем все звонки по времени начала
        allCdrRecords.sort(Comparator.comparing(CdrRecord::getStartTime));

        // Сохраняем звонки в базу данных
        cdrRecordRepository.saveAll(allCdrRecords);
    }
}
