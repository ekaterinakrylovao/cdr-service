package com.example.cdrservice;

import com.example.cdrservice.entity.Subscriber;
import com.example.cdrservice.repository.CdrRecordRepository;
import com.example.cdrservice.repository.SubscriberRepository;
import com.example.cdrservice.service.CdrGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private CdrRecordRepository cdrRecordRepository;

    @Autowired
    private CdrGeneratorService cdrGeneratorService;

    @Override
    public void run(String... args) throws Exception {
        // Очищаем таблицы перед генерацией данных
        cdrRecordRepository.deleteAll(); // Очистка CDR_RECORD
        subscriberRepository.deleteAll(); // Очистка SUBSCRIBER

        // Создаем список абонентов
        List<String> msisdns = Arrays.asList(
                "79991112233", "79992221122", "79993334455", "79994445566",
                "79995556677", "79996667788", "79997778899", "79998889900",
                "79990001122", "79991113344"
        );

        for (String msisdn : msisdns) {
            Subscriber subscriber = new Subscriber();
            subscriber.setMsisdn(msisdn);
            subscriberRepository.save(subscriber);
        }

        // Генерируем CDR-записи
        cdrGeneratorService.generateCdrRecords();
    }
}
