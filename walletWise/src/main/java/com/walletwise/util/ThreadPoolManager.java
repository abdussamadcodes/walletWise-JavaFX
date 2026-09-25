package com.walletwise.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Concurrency: Thread Pool implementation.
 * We will use this to run database and API calls off the main JavaFX UI thread.
 */
public class ThreadPoolManager {
    // Creates a pool of threads that can be reused
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void execute(Runnable task) {
        executor.submit(task);
    }

    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}