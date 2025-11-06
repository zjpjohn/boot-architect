package com.cloud.arch.hotkey.utils;

import cn.hutool.core.date.SystemClock;

import java.util.concurrent.atomic.AtomicLong;

public class SlidingWindow {

    /**
     * 时间窗口
     */
    private       AtomicLong[] timeSlices;
    /**
     * 统计时间片数量(2*windowSize)
     */
    private final int          timeSliceSize;
    /**
     * 每个时间片时长，单位毫秒
     */
    private final int          timeMillisPerSlice;
    /**
     * 时间窗口长度，共多少个时间片
     */
    private final int          windowSize;
    /**
     * 在一个完整时间窗口允许通过的阈值
     */
    private final int          threshold;
    /**
     * 活动窗其实创建时间，第一个数据
     */
    private       long         beginTimestamp;
    /**
     * 最后一个数据的时间戳
     */
    private       long         lastAddTimestamp;

    public SlidingWindow(int duration, int threshold) {
        this.threshold = threshold;
        duration       = Math.min(600, duration);
        if (duration <= 5) {
            this.windowSize         = 5;
            this.timeMillisPerSlice = duration * 200;
        } else {
            this.windowSize         = 10;
            this.timeMillisPerSlice = duration * 100;
        }
        this.timeSliceSize = windowSize * 2;
        //初始化时间窗
        this.reset();
    }

    private void reset() {
        beginTimestamp = SystemClock.now();
        //窗口个数
        AtomicLong[] localTimeSlices = new AtomicLong[timeSliceSize];
        for (int i = 0; i < timeSliceSize; i++) {
            localTimeSlices[i] = new AtomicLong(0);
        }
        timeSlices = localTimeSlices;
    }

    private int locateIndex() {
        long now = SystemClock.now();
        if (now - lastAddTimestamp > (long) timeMillisPerSlice * windowSize) {
            reset();
        }
        int index = (int) (((now - beginTimestamp) / timeMillisPerSlice) % timeSliceSize);
        return Math.max(0, index);
    }

    /**
     * 增加count个数量
     */
    public synchronized boolean addCount(long count) {
        //当前自己所在的位置，是哪个小时间窗
        int index = locateIndex();
        //然后清空自己前面windowSize到2*windowSize之间的数据格的数据
        //譬如1秒分4个窗口，那么数组共计8个窗口
        //当前index为5时，就清空6、7、8、1。然后把2、3、4、5的加起来就是该窗口内的总和
        clearFromIndex(index);
        int sum = 0;
        // 在当前时间片里继续+1
        sum += timeSlices[index].addAndGet(count);
        //加上前面几个时间片
        for (int i = 1; i < windowSize; i++) {
            sum += timeSlices[(index - i + timeSliceSize) % timeSliceSize].get();
        }
        lastAddTimestamp = SystemClock.now();
        return sum >= threshold;
    }

    private void clearFromIndex(int index) {
        for (int i = 1; i <= windowSize; i++) {
            int j = index + i;
            if (j >= windowSize * 2) {
                j -= windowSize * 2;
            }
            timeSlices[j].set(0);
        }
    }


}
