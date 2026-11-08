package com.banking.customer_service.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Replaces the system clock so every operation gets a distinct, predictable timestamp. */
@TestConfiguration(proxyBeanMethods = false)
public class SteppingClockConfiguration {

    public static final Instant START = Instant.parse("2026-11-05T15:00:00Z");

    @Bean
    @Primary
    Clock steppingClock() {
        return new SteppingClock(START, Duration.ofSeconds(1));
    }
}
