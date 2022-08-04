package com.iot.mqtt.statistics;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author liangjiajun
 */
@Slf4j
public class QpsStatistics {

    private static AtomicLong beginTime = new AtomicLong(0);
    private static AtomicLong totalResponseTime = new AtomicLong(0);
    private static AtomicInteger totalRequest = new AtomicInteger(0);

    public QpsStatistics() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            long duration = System.currentTimeMillis() - beginTime.get();
            if (duration != 0) {
                log.info("qps: " + 1000L * totalRequest.get() / duration + ", " + "avg response time: " + ((float) totalResponseTime.get()) / totalRequest.get());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void cost(Long costTime) {
        totalResponseTime.addAndGet(costTime);
        totalRequest.incrementAndGet();
    }
}
