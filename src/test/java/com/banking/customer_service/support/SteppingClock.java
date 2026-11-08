package com.banking.customer_service.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Clock that advances a fixed step on every read, so each operation gets a
 * distinct, predictable timestamp. Thread safe.
 */
public final class SteppingClock extends Clock {

    private final Instant start;
    private final Duration step;
    private final AtomicLong reads = new AtomicLong();

    public SteppingClock(Instant start, Duration step) {
        this.start = start;
        this.step = step;
    }

    @Override
    public Instant instant() {
        return start.plus(step.multipliedBy(reads.getAndIncrement()));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("SteppingClock is always UTC.");
    }
}
