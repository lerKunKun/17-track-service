package com.example.trackservice.client;

import com.example.trackservice.dto.TrackQueryRequest;
import com.example.trackservice.dto.TrackResponse;
import com.example.trackservice.dto.TrackResponse.TrackingEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
public class SeventeenTrackClient {

    private static final Logger log = LoggerFactory.getLogger(SeventeenTrackClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String apiSecret;

    public SeventeenTrackClient(WebClient trackingWebClient,
                                @Value("${integration.17track.api-key}") String apiKey,
                                @Value("${integration.17track.api-secret}") String apiSecret) {
        this.webClient = trackingWebClient;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public Mono<TrackResponse> queryAsync(TrackQueryRequest request) {
        return webClient.post()
                .uri("/gettrackinfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-17Track-Secret", apiSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TrackResponse.class)
                .doOnError(error -> log.error("17TRACK query failed for {}", request.getTrackingNumber(), error))
                .onErrorResume(error -> Mono.just(mockResponse(request)));
    }

    private TrackResponse mockResponse(TrackQueryRequest request) {
        TrackingEventDto event = new TrackingEventDto(Instant.now(), "CN", "Mock in-transit", "InTransit");
        return new TrackResponse(
                request.getTrackingNumber(),
                request.getCarrierCode(),
                request.getReferenceNumber(),
                "Mock",
                "Mock fallback response",
                Instant.now(),
                Instant.now(),
                List.of(event)
        );
    }
}
