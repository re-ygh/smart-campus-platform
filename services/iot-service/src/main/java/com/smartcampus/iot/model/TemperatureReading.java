package com.smartcampus.iot.model;

import java.time.LocalDateTime;

public record TemperatureReading(String room, double value, LocalDateTime timestamp) {
}

