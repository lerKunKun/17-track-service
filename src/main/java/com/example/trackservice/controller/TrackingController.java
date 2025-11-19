package com.example.trackservice.controller;

import com.example.trackservice.dto.TrackQueryRequest;
import com.example.trackservice.dto.TrackResponse;
import com.example.trackservice.dto.WebhookPayload;
import com.example.trackservice.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracks")
@Validated
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackResponse> getTracking(@PathVariable String trackingNumber) {
        TrackResponse response = trackingService.getCachedTracking(trackingNumber);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/query")
    public ResponseEntity<TrackResponse> query(@Valid @RequestBody TrackQueryRequest request) {
        TrackResponse response = trackingService.queryAndPersist(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody WebhookPayload payload) {
        trackingService.processWebhook(payload);
        return ResponseEntity.accepted().build();
    }
}
