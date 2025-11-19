package com.example.trackservice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tracking_events")
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private TrackingRecord record;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "location", length = 128)
    private String location;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "status", length = 128)
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrackingRecord getRecord() {
        return record;
    }

    public void setRecord(TrackingRecord record) {
        this.record = record;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
