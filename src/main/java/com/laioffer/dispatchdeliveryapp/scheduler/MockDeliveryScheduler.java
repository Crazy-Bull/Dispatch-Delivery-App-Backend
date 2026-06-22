package com.laioffer.dispatchdeliveryapp.scheduler;

import com.laioffer.dispatchdeliveryapp.config.MockDeliveryProperties;
import com.laioffer.dispatchdeliveryapp.service.MockDeliveryService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(MockDeliveryProperties.class)
public class MockDeliveryScheduler {

    private final MockDeliveryService mockDeliveryService;
    private final MockDeliveryProperties properties;

    public MockDeliveryScheduler(
            MockDeliveryService mockDeliveryService,
            MockDeliveryProperties properties) {
        this.mockDeliveryService = mockDeliveryService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${mock-delivery.tick-interval-ms:5000}")
    public void runTick() {
        if (!properties.enabled()) {
            return;
        }
        mockDeliveryService.tick();
    }
}
