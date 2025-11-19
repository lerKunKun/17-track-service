package com.example.trackservice.repository;

import com.example.trackservice.model.TrackingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackingRecordRepository extends JpaRepository<TrackingRecord, Long> {

    Optional<TrackingRecord> findByTrackingNumber(String trackingNumber);
}
