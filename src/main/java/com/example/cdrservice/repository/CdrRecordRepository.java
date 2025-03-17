package com.example.cdrservice.repository;

import com.example.cdrservice.entity.CdrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CdrRecordRepository extends JpaRepository<CdrRecord, Long> {
    @Query("SELECT MIN(r.startTime) FROM CdrRecord r")
    LocalDateTime findEarliestStartTime();

    @Query("SELECT MAX(r.endTime) FROM CdrRecord r")
    LocalDateTime findLatestEndTime();

    List<CdrRecord> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<CdrRecord> findByCallerNumberAndStartTimeBetweenOrReceiverNumberAndStartTimeBetween(
            String callerNumber, LocalDateTime start1, LocalDateTime end1,
            String receiverNumber, LocalDateTime start2, LocalDateTime end2);
}
