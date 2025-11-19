package com.example.trackservice.repository;

import com.example.trackservice.model.TrackingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrackingRecordRepository extends JpaRepository<TrackingRecord, Long> {

    Optional<TrackingRecord> findByTrackingNumber(String trackingNumber);

    @Query("""
            select r from TrackingRecord r
            where r.lastSyncedAt is null
               or (r.lastEventTime is not null and r.lastSyncedAt < r.lastEventTime)
            """)
    List<TrackingRecord> findRecordsNeedingRefresh();
}
