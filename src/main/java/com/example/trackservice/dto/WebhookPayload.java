package com.example.trackservice.dto;

import java.time.Instant;
import java.util.List;

public class WebhookPayload {

    private String trackingNumber;
    private String carrierCode;
    private String referenceNumber;
    private List<WebhookEvent> events;

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public List<WebhookEvent> getEvents() {
        return events;
    }

    public void setEvents(List<WebhookEvent> events) {
        this.events = events;
    }

    public static class WebhookEvent {
        private Instant eventTime;
        private String location;
        private String description;
        private String status;

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
}
