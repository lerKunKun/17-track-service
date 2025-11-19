package com.example.trackservice.service;

import com.example.trackservice.client.SeventeenTrackClient;
import com.example.trackservice.dto.TrackQueryRequest;
import com.example.trackservice.dto.TrackResponse;
import com.example.trackservice.dto.TrackResponse.TrackingEventDto;
import com.example.trackservice.dto.WebhookPayload;
import com.example.trackservice.model.TrackingEvent;
import com.example.trackservice.model.TrackingRecord;
import com.example.trackservice.repository.TrackingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final TrackingRecordRepository repository;
    private final SeventeenTrackClient client;

    public TrackingService(TrackingRecordRepository repository, SeventeenTrackClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Cacheable(cacheNames = "tracking", key = "#trackingNumber")
    public TrackResponse getCachedTracking(String trackingNumber) {
        Optional<TrackingRecord> optional = repository.findByTrackingNumber(trackingNumber);
        return optional.map(this::mapToResponse).orElse(null);
    }

    @Transactional
    public TrackResponse queryAndPersist(TrackQueryRequest request) {
        TrackResponse response = client.queryAsync(request).block();
        if (response == null) {
            throw new IllegalStateException("Empty response from 17TRACK");
        }
        TrackingRecord record = repository.findByTrackingNumber(response.trackingNumber())
                .orElseGet(TrackingRecord::new);
        record.setTrackingNumber(response.trackingNumber());
        record.setCarrierCode(response.carrierCode());
        record.setReferenceNumber(response.referenceNumber());
        record.setLatestStatus(response.latestStatus());
        record.setStatusDetail(response.statusDetail());
        record.setLastEventTime(response.lastEventTime());
        record.setLastSyncedAt(Instant.now());
        record.getEvents().clear();
        if (response.events() != null) {
            response.events().forEach(eventDto -> record.addEvent(mapEvent(eventDto)));
        }
        repository.save(record);
        return mapToResponse(record);
    }

    @Transactional
    public void processWebhook(WebhookPayload payload) {
        TrackingRecord record = repository.findByTrackingNumber(payload.getTrackingNumber())
                .orElseGet(TrackingRecord::new);
        record.setTrackingNumber(payload.getTrackingNumber());
        record.setCarrierCode(payload.getCarrierCode());
        record.setReferenceNumber(payload.getReferenceNumber());
        if (payload.getEvents() != null && !payload.getEvents().isEmpty()) {
            record.getEvents().clear();
            payload.getEvents().forEach(event -> {
                TrackingEvent entity = new TrackingEvent();
                entity.setEventTime(event.getEventTime());
                entity.setLocation(event.getLocation());
                entity.setDescription(event.getDescription());
                entity.setStatus(event.getStatus());
                record.addEvent(entity);
                record.setLatestStatus(event.getStatus());
                record.setStatusDetail(event.getDescription());
                record.setLastEventTime(event.getEventTime());
            });
        }
        record.setLastSyncedAt(Instant.now());
        repository.save(record);
        log.info("Webhook processed for tracking {}", record.getTrackingNumber());
    }

    private TrackResponse mapToResponse(TrackingRecord record) {
        List<TrackingEventDto> events = record.getEvents().stream()
                .map(event -> new TrackingEventDto(event.getEventTime(), event.getLocation(), event.getDescription(), event.getStatus()))
                .toList();
        return new TrackResponse(
                record.getTrackingNumber(),
                record.getCarrierCode(),
                record.getReferenceNumber(),
                record.getLatestStatus(),
                record.getStatusDetail(),
                record.getLastEventTime(),
                record.getLastSyncedAt(),
                events
        );
    }

    private TrackingEvent mapEvent(TrackingEventDto dto) {
        TrackingEvent event = new TrackingEvent();
        event.setEventTime(dto.eventTime());
        event.setLocation(dto.location());
        event.setDescription(dto.description());
        event.setStatus(dto.status());
        return event;
    }
}
