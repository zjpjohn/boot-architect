package com.boot.architect;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 模拟赛跑，要求满足以下三点：
 * 1.所有运动员准备就绪告知裁判已准备好，可以开始发布开始比赛指令
 * 2.待所有运动员准备就绪后，裁判发布指令开始赛跑
 * 3.所有运动员到达终点，告知裁判比赛结束，整个比赛结束
 */
public class RunningDemo {

    public static void main(String[] args) {
        CountDownLatch ready  = new CountDownLatch(5);
        CountDownLatch end    = new CountDownLatch(5);
        CountDownLatch start  = new CountDownLatch(1);
        Random         random = new Random();
        //裁判线程
        new Thread(() -> {
            try {
                ready.await();
                System.out.println("预备开始");
                start.countDown();
                end.await();
                System.out.println("比赛结束");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        for (int i = 1; i <= 5; i++) {
            final int num = i;
            //运动员线程
            new Thread(() -> {
                try {
                    System.out.println("运动员" + num + "准备中..");
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println("运动员" + num + "准备就绪");
                    ready.countDown();
                    start.await();
                    System.out.println("运动员" + num + "开始赛跑");
                    TimeUnit.SECONDS.sleep(random.nextInt(10));
                    System.out.println("运动员" + num + "到达终点.");
                    end.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
}
