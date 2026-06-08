package com.cloud.arch.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SingleFlight 请求合并")
class SingleFlightTest {

    private SingleFlight<String, String> sf;

    @BeforeEach
    void setUp() {
        sf = new SingleFlight<>();
    }

    @Nested
    @DisplayName("基本功能")
    class Basic {

        @Test
        @DisplayName("单次执行返回正确结果")
        void shouldReturnCallableResult() throws Exception {
            String result = sf.execute("key1", () -> "value1");
            assertThat(result).isEqualTo("value1");
        }

        @Test
        @DisplayName("不同 key 各自独立执行")
        void shouldExecuteIndependentlyForDifferentKeys() throws Exception {
            String r1 = sf.execute("k1", () -> "v1");
            String r2 = sf.execute("k2", () -> "v2");
            assertThat(r1).isEqualTo("v1");
            assertThat(r2).isEqualTo("v2");
        }

        @Test
        @DisplayName("同一 key 先后两次调用各自执行（首次完成后 key 已移除）")
        void shouldExecuteSeparatelyWhenCalledSequentially() throws Exception {
            String r1 = sf.execute("key", () -> "first");
            String r2 = sf.execute("key", () -> "second");
            assertThat(r1).isEqualTo("first");
            assertThat(r2).isEqualTo("second");
        }
    }

    @Nested
    @DisplayName("并发请求合并")
    class Concurrency {

        @Test
        @DisplayName("同一 key 并发调用只执行一次 Callable，所有调用者获得相同结果")
        void shouldMergeConcurrentCallsForSameKey() throws Exception {
            int threadCount = 10;
            AtomicInteger callCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<String> results = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        String r = sf.execute("shared-key", () -> {
                            callCount.incrementAndGet();
                            Thread.sleep(100);
                            return "shared-value";
                        });
                        synchronized (results) {
                            results.add(r);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertThat(callCount.get()).isEqualTo(1);
            assertThat(results).hasSize(threadCount)
                               .allMatch(r -> "shared-value".equals(r));
        }

        @Test
        @DisplayName("不同 key 并发调用各自独立执行")
        void shouldNotMergeDifferentKeys() throws Exception {
            int threadCount = 5;
            AtomicInteger callCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<String> results = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final String key = "key-" + i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        String r = sf.execute(key, () -> {
                            callCount.incrementAndGet();
                            return "value-" + key;
                        });
                        synchronized (results) {
                            results.add(r);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertThat(callCount.get()).isEqualTo(threadCount);
            assertThat(results).hasSize(threadCount);
        }
    }

    @Nested
    @DisplayName("异常传播")
    class ExceptionPropagation {

        @Test
        @DisplayName("Callable 抛异常 → 执行线程和等待线程都收到相同异常")
        void shouldPropagateExceptionToAllCallers() throws Exception {
            int threadCount = 5;
            RuntimeException theException = new RuntimeException("boom");
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            List<Exception> errors = new ArrayList<>();

            AtomicInteger executeCount = new AtomicInteger(0);
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        sf.execute("err-key", () -> {
                            executeCount.incrementAndGet();
                            Thread.sleep(100);
                            throw theException;
                        });
                    } catch (Exception e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertThat(executeCount.get()).isEqualTo(1);
            assertThat(errors).hasSize(threadCount)
                              .allMatch(e -> e == theException || e.getCause() == theException);
        }

        @Test
        @DisplayName("执行抛异常后 key 被移除，后续调用可重新执行")
        void shouldAllowRetryAfterException() throws Exception {
            AtomicInteger attempt = new AtomicInteger(0);

            assertThatThrownBy(() -> sf.execute("retry-key", () -> {
                attempt.incrementAndGet();
                throw new RuntimeException("fail");
            })).isInstanceOf(RuntimeException.class);

            // 异常后 key 已移除，第二次调用重新执行
            String result = sf.execute("retry-key", () -> {
                attempt.incrementAndGet();
                return "recovered";
            });

            assertThat(attempt.get()).isEqualTo(2);
            assertThat(result).isEqualTo("recovered");
        }
    }

    @Nested
    @DisplayName("边界条件")
    class EdgeCases {

        @Test
        @DisplayName("返回 null 是合法结果")
        void shouldSupportNullResult() throws Exception {
            String result = sf.execute("null-key", () -> null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Callable 返回 null 后等待者也能正确收到 null")
        void shouldPropagateNullToWaitingCallers() throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            List<Object> results = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Object r = sf.execute("null-shared", () -> {
                            Thread.sleep(100);
                            return null;
                        });
                        synchronized (results) {
                            results.add(r);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            assertThat(results).hasSize(2).allMatch(r -> r == null);
        }
    }
}
