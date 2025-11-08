package com.boot.architect;

import java.util.concurrent.*;

public class RunningDemo {

    public static void main(String[] args) {
        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch end   = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(false); // 设置为非守护线程
            return t;
        });

        // 裁判线程
        executor.submit(() -> {
            try {
                ready.await();
                System.out.println("预备开始");
                start.countDown();
                end.await();
                System.out.println("比赛结束");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("裁判线程被中断", e);
            }
        });

        for (int i = 1; i <= 5; i++) {
            final int num = i;
            // 运动员线程
            executor.submit(() -> {
                try {
                    System.out.println("运动员" + num + "准备中..");
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println("运动员" + num + "准备就绪");
                    ready.countDown();
                    start.await();
                    System.out.println("运动员" + num + "开始赛跑");

                    int sleepTime = ThreadLocalRandom.current().nextInt(10);
                    TimeUnit.SECONDS.sleep(sleepTime);

                    System.out.println("运动员" + num + "到达终点.");
                    end.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("运动员" + num + "线程被中断", e);
                }
            });
        }

        executor.shutdown(); // 关闭线程池
    }
}
