package com.example.trackservice.listener;

import com.example.trackservice.dto.TrackQueryRequest;
import com.example.trackservice.model.TrackingRecord;
import com.example.trackservice.repository.TrackingRecordRepository;
import com.example.trackservice.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrackingRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrackingRefreshScheduler.class);

    private final TrackingRecordRepository repository;
    private final TrackingService trackingService;

    public TrackingRefreshScheduler(TrackingRecordRepository repository, TrackingService trackingService) {
        this.repository = repository;
        this.trackingService = trackingService;
    }

    @Scheduled(fixedDelayString = "${integration.refresh-interval-ms:60000}")
    public void refreshActiveShipments() {
        repository.findRecordsNeedingRefresh()
                .forEach(this::refreshRecord);
    }

    private void refreshRecord(TrackingRecord record) {
        try {
            TrackQueryRequest request = new TrackQueryRequest();
            request.setTrackingNumber(record.getTrackingNumber());
            request.setCarrierCode(record.getCarrierCode());
            request.setReferenceNumber(record.getReferenceNumber());
            trackingService.queryAndPersist(request);
        } catch (Exception ex) {
            log.error("Failed to refresh tracking {}", record.getTrackingNumber(), ex);
        }
    }
}
