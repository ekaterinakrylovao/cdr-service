package com.example.cdrservice.repository;

import com.example.cdrservice.entity.CdrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CdrRecordRepository extends JpaRepository<CdrRecord, Long> {

    @Query("SELECT r FROM CdrRecord r WHERE (r.callerNumber = :msisdn OR r.receiverNumber = :msisdn) AND r.startTime BETWEEN :start AND :end")
    List<CdrRecord> findRecordsForMsisdnInPeriod(String msisdn, LocalDateTime start, LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(r) = 0 THEN true ELSE false END FROM CdrRecord r WHERE r.callerNumber = :msisdn OR r.receiverNumber = :msisdn")
    boolean doesNotExistByCallerNumberOrReceiverNumber(String msisdn);

    @Query("SELECT MIN(r.startTime) FROM CdrRecord r")
    LocalDateTime findEarliestStartTime();

    @Query("SELECT MAX(r.endTime) FROM CdrRecord r")
    LocalDateTime findLatestEndTime();

    List<CdrRecord> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
