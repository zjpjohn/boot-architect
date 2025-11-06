/*
 * All rights Reserved, Designed By baowei
 *
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的
 */
package com.cloud.arch.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class IdWorker {

    /**
     * 进制转换字符串集合
     */
    private static final String RADIX_CHARS = "2AaJRr3BbKkSs4CcLlTt5DdMmUu6EeNnVv7FfPWw8GXx9HhYZz";

    /**
     * 起始的时间戳
     */
    private static final long START_TIMESTAMP = 1561278837352L;

    /**
     * 毫秒内自增位：10位支持1024个
     */
    private static final long SEQUENCE_BITS = 10L;

    /**
     * IP标识位数
     */
    private static final long IP_BITS = 8L;

    /**
     * PID标识位数
     */
    private static final long PID_BITS = 5L;

    /**
     * 机器标识位数：13位支持8192台机器
     */
    private static final long WORKER_ID_BITS = IP_BITS + PID_BITS;

    /**
     * 时间毫秒左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 序列号左移位数
     */
    private static final long SEQUENCE_SHIFT = WORKER_ID_BITS;

    /**
     * 序列号最大值
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 上次生成ID时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 机器ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    private static final IdWorker WORKER = new IdWorker();

    private IdWorker() {
        long id = getWorkerId();
        if (id > MAX_WORKER_ID || id < 0) {
            throw new RuntimeException(String.format("worker Id can't be greater than %d or less than 0",
                                                     MAX_WORKER_ID));
        }
        this.workerId = id;
    }

    private long getNextId() {
        synchronized (this) {
            long timestamp = currentTimestamp();
            if (this.lastTimestamp == timestamp) {
                this.sequence = (this.sequence + 1) & MAX_SEQUENCE;
                if (this.sequence == 0) {
                    timestamp = tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0;
            }
            if (timestamp < this.lastTimestamp) {
                log.error("Clock moved backwards. Refusing to generate id for {} milliseconds",
                          this.lastTimestamp - timestamp);
            }
            this.lastTimestamp = timestamp;
            return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) | (this.sequence << SEQUENCE_SHIFT)
                   | this.workerId;
        }
    }

    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = currentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimestamp();
        }
        return timestamp;
    }

    private long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取long型 uuid
     */
    public static long nextId() {
        return WORKER.getNextId();
    }

    /**
     * 转换进制的字符型uuid
     */
    public static String uuid() {
        long          uid       = WORKER.getNextId();
        int           remainder = 0, radix = RADIX_CHARS.length();
        StringBuilder builder   = new StringBuilder();
        while (uid > radix - 1) {
            remainder = Long.valueOf(uid % radix).intValue();
            builder.append(RADIX_CHARS.charAt(remainder));
            uid = uid / radix;
        }
        builder.append(RADIX_CHARS.charAt((int) uid));
        return builder.toString();
    }

    /**
     * 生成机器ID
     */
    private static long getWorkerId() {
        byte[] ip;
        long   workerId;
        try {
            ip       = getPrivateIp().getAddress();
            workerId = (ip[2] << 8 | (int) ip[3]) & ((1 << IP_BITS) - 1);
        } catch (SocketException e) {
            throw new RuntimeException("无法获取本机IP地址", e);
        }
        long pid = getPid() & ((1 << PID_BITS) - 1);
        return (workerId << PID_BITS) | pid;
    }

    /**
     * 取得本机内网IP
     */
    private static InetAddress getPrivateIp() throws SocketException {
        List<InetAddress> addresses = getAllIPs();
        if (addresses.isEmpty()) {
            throw new RuntimeException("无法获取本机IP地址");
        }
        List<InetAddress> localAddresses = addresses.stream()
                                                    .filter(InetAddress::isSiteLocalAddress)
                                                    .collect(Collectors.toList());
        if (localAddresses.isEmpty()) {
            throw new RuntimeException("无法获取本机内网IP地址");
        }
        return localAddresses.get(0);
    }

    /**
     * 取得本机所有IP
     */
    private static List<InetAddress> getAllIPs() throws SocketException {
        List<InetAddress>             result = new ArrayList<>();
        Enumeration<NetworkInterface> netInterfaces;
        netInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip;
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface         ni        = netInterfaces.nextElement();
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                ip = addresses.nextElement();
                if (!ip.getHostAddress().contains(":")) {
                    result.add(ip);
                }
            }
        }
        return result;
    }

    /**
     * 取得PID
     */
    private static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String        name    = runtime.getName();
        int           index   = name.indexOf('@');
        if (index == -1) {
            throw new RuntimeException("获取PID错误, name=" + name);
        }
        try {
            return Integer.parseInt(name.substring(0, index));
        } catch (NumberFormatException e) {
            throw new RuntimeException("获取PID错误, name=" + name, e);
        }
    }

}
