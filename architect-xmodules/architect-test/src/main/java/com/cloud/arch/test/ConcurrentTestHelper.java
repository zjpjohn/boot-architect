package com.cloud.arch.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发测试辅助工具，封装多线程执行与结果验证的通用模式。
 *
 * <pre>{@code
 * // 验证：N 个并发请求中只有一个成功
 * String result = ConcurrentTestHelper.assertExactlyOneSuccess(10, () -> service.process(id));
 *
 * // 验证：所有并发执行结果一致
 * ConcurrentTestHelper.assertAllEqual(20, () -> cache.get(key));
 * }</pre>
 */
public final class ConcurrentTestHelper {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private ConcurrentTestHelper() {
    }

    /**
     * N 个线程同时执行同一个任务，收集所有正常完成的结果。
     */
    public static <T> List<T> executeConcurrently(int threadCount, Callable<T> task) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<T> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    T result = task.call();
                    synchronized (results) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    synchronized (results) {
                        results.add(null);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        try {
            doneLatch.await(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
        return results;
    }

    /**
     * 验证：N 个线程同时执行，恰好只有一个成功返回非 null 结果。
     * 适用于幂等逻辑、分布式锁正确性测试。
     */
    public static <T> T assertExactlyOneSuccess(int threadCount, Callable<T> task) {
        List<T> results = executeConcurrently(threadCount, task);
        long successCount = results.stream().filter(r -> r != null).count();
        assertThat(successCount).as("Expected exactly 1 success out of %d concurrent calls", threadCount)
                                .isEqualTo(1);
        return results.stream().filter(r -> r != null).findFirst().orElse(null);
    }

    /**
     * 验证：N 个线程同时执行，所有结果一致。
     * 适用于缓存一致性、ID 幂等等测试。
     */
    public static <T> void assertAllEqual(int threadCount, Callable<T> task) {
        List<T> results = executeConcurrently(threadCount, task);
        assertThat(results).isNotEmpty();
        T first = results.get(0);
        assertThat(results).allMatch(r -> r != null && r.equals(first),
                                     String.format("Expected all %d results to equal %s", threadCount, first));
    }

    /**
     * 让 startLatch 统一释放的并发执行封装。
     * 在 startLatch.await() 之前给每个线程一次 init 机会（延迟初始化的场景）。
     */
    public static <T> T runWithInit(Callable<T> init, Callable<T> task, int concurrentCount) {
        try {
            T prepared = init.call();
            List<T> results = executeConcurrently(concurrentCount, task);
            return prepared;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
