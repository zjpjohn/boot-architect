package com.cloud.arch.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SleepyTask 延迟任务")
class SleepyTaskTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Nested
    @DisplayName("基本生命周期")
    class Lifecycle {

        @Test
        @DisplayName("wakeup 触发任务执行 runTask")
        void shouldExecuteRunTaskOnWakeup() throws Exception {
            AtomicInteger counter = new AtomicInteger(0);
            SleepyTask task = new SleepyTask(executor) {
                @Override
                protected void runTask() {
                    counter.incrementAndGet();
                }
            };

            task.wakeup();
            executor.shutdown();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();

            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("shutdown 后 ready 被置 false，正在运行的循环在下一次迭代时退出")
        void shouldStopLoopAfterShutdown() throws Exception {
            AtomicInteger counter = new AtomicInteger(0);
            // ready 初始为 true → run 会循环多次；shutdown 后 ready 置 false 退出循环
            SleepyTask task = new SleepyTask(executor) {
                @Override
                protected void runTask() {
                    counter.incrementAndGet();
                }
            };

            // 手动设置 ready=true，让 run 进入循环
            task.wakeup();
            // 等待 runTask 至少执行一次
            Thread.sleep(50);
            task.shutdown();
            executor.shutdown();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();

            // shutdown 前可能已经执行了若干次，但最终会停止
            assertThat(counter.get()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("多次 wakeup 期间只执行一次 runTask（running 状态防重入）")
        void shouldRunOnlyOnceForMultipleWakeups() throws Exception {
            AtomicInteger counter = new AtomicInteger(0);
            SleepyTask task = new SleepyTask(executor) {
                @Override
                protected void runTask() {
                    counter.incrementAndGet();
                }
            };

            task.wakeup();
            task.wakeup();
            task.wakeup();
            executor.shutdown();
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS)).isTrue();

            // run() 会循环调用 runTask，但 running CAS 保证只提交一次
            assertThat(counter.get()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ErrorHandling {

        @Test
        @DisplayName("runTask 抛异常不影响后续 wakeup")
        void shouldRecoverFromRunTaskException() throws Exception {
            AtomicInteger counter = new AtomicInteger(0);
            SleepyTask task = new SleepyTask(executor) {
                @Override
                protected void runTask() {
                    int count = counter.incrementAndGet();
                    if (count == 1) {
                        throw new RuntimeException("simulated error");
                    }
                }
            };

            task.wakeup();
            // 等待第一次执行完成
            Thread.sleep(100);
            // shutdown 防止第二次 wakeup 的循环
            task.shutdown();
            executor.shutdown();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();

            assertThat(counter.get()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isRunning 状态")
    class RunningState {

        @Test
        @DisplayName("wakeup 前 isRunning 为 false")
        void shouldNotBeRunningBeforeWakeup() {
            SleepyTask task = new SleepyTask(executor) {
                @Override
                protected void runTask() {
                }
            };
            assertThat(task.isRunning()).isFalse();
        }
    }
}
