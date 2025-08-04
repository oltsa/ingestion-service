package com.oltsa.email.ingestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Test-specific Spring configuration that overrides the default async behavior.
 * This class is only active during the 'test' profile.
 */
@Configuration
public class TestAsyncConfig {

    /**
     * Creates a synchronous task executor bean.
     * When this bean is present, any method marked with @Async will execute
     * on the calling thread, blocking until it is complete. This makes tests
     * deterministic and prevents them from hanging.
     * The @Primary annotation ensures this bean is used over any other Executor.
     * @return A synchronous task executor.
     */
    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}