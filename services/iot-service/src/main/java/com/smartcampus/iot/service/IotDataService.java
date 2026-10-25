package com.smartcampus.iot.service;

import com.smartcampus.iot.model.ShuttleLocation;
import com.smartcampus.iot.model.TemperatureReading;
import com.smartcampus.iot.tenant.TenantContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IotDataService {

    private final Map<String, List<TemperatureReading>> readings = new ConcurrentHashMap<>();
    private final Map<String, ShuttleLocation> shuttles = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Scheduled(fixedRate = 15000)
    public void generate() {
        // generate demo data for default tenant
        generateForTenant("faculty-a");
    }

    private void generateForTenant(String tenantId) {
        readings.computeIfAbsent(tenantId, t -> new ArrayList<>())
                .add(new TemperatureReading("Room-" + (1 + random.nextInt(3)),
                        20 + random.nextDouble(5),
                        LocalDateTime.now()));
        shuttles.put(tenantId, new ShuttleLocation(35.7 + random.nextDouble(0.01), 51.3 + random.nextDouble(0.01), 25 + random.nextDouble(5)));
        if (readings.get(tenantId).size() > 50) {
            readings.get(tenantId).remove(0);
        }
    }

    public List<TemperatureReading> temperatures() {
        String tenant = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : "faculty-a";
        return List.copyOf(readings.getOrDefault(tenant, List.of()));
    }

    public ShuttleLocation shuttle() {
        String tenant = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : "faculty-a";
        return shuttles.getOrDefault(tenant, new ShuttleLocation(0, 0, 0));
    }
}

