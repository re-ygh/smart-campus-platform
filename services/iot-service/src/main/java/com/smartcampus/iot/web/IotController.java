package com.smartcampus.iot.web;

import com.smartcampus.iot.model.ShuttleLocation;
import com.smartcampus.iot.model.TemperatureReading;
import com.smartcampus.iot.service.IotDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/iot")
@RequiredArgsConstructor
public class IotController {

    private final IotDataService iotDataService;

    @GetMapping("/temperatures")
    public ResponseEntity<List<TemperatureReading>> temps() {
        return ResponseEntity.ok(iotDataService.temperatures());
    }

    @GetMapping("/shuttle")
    public ResponseEntity<ShuttleLocation> shuttle() {
        return ResponseEntity.ok(iotDataService.shuttle());
    }
}

