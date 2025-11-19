package com.example.trackservice.dto;

import java.time.Instant;
import java.util.List;

public record TrackResponse(
        String trackingNumber,
        String carrierCode,
        String referenceNumber,
        String latestStatus,
        String statusDetail,
        Instant lastEventTime,
        Instant lastSyncedAt,
        List<TrackingEventDto> events
) {
    public record TrackingEventDto(Instant eventTime, String location, String description, String status) {}
}
