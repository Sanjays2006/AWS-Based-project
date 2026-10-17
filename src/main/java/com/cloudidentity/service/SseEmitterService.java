package com.cloudidentity.service;

import com.cloudidentity.entity.AnomalyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of("message", "Connected to CloudIdentity live threat stream", "timestamp", System.currentTimeMillis())));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcastAnomaly(AnomalyEvent anomaly) {
        if (emitters.isEmpty()) return;

        Map<String, Object> data = Map.of(
                "id", anomaly.getId(),
                "identityName", anomaly.getIamIdentity() != null ? anomaly.getIamIdentity().getIdentityName() : "Unknown",
                "anomalyType", anomaly.getAnomalyType(),
                "severity", anomaly.getSeverity(),
                "description", anomaly.getDescription() != null ? anomaly.getDescription() : "",
                "riskScore", anomaly.getRiskDelta() != null ? anomaly.getRiskDelta() : 0.0,
                "createdAt", anomaly.getCreatedAt().toString()
        );

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("THREAT_ALERT")
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }
}
