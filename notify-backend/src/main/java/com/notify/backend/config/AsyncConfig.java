package com.notify.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Dedicated thread pool for bulk upload processing.
     * Small pool: bulk uploads are IO-bound (DB + Kafka), so a few threads handle
     * concurrent uploads without overwhelming the connection pool.
     */
    @Bean("bulkUploadExecutor")
    public Executor bulkUploadExecutor() {
        return new ThreadPoolExecutor(
                2,                          // core threads always alive
                4,                          // max threads under load
                60L, TimeUnit.SECONDS,      // idle thread keepalive
                new LinkedBlockingQueue<>(100),  // queue up to 100 pending uploads
                r -> {
                    Thread t = new Thread(r, "bulk-upload-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // back-pressure: caller processes if queue is full
        );
    }
}
