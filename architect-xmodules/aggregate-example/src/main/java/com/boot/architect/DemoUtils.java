package com.boot.architect;

import org.apache.commons.lang3.tuple.Triple;

public class DemoUtils {

    //一瓶饮料11元
    private static final Integer price = 11;
    //空瓶5元
    private static final Integer empty = 5;
    //两个瓶盖3元
    private static final Integer cap   = 3;

    public static Triple<Integer, Integer, Integer> calc(Integer value) {
        return calc(value, 0, 0);
    }

    /**
     * @param value 余钱
     * @param caps  瓶盖数
     * @param count 已喝饮料瓶数
     */
    private static Triple<Integer, Integer, Integer> calc(Integer value, Integer caps, Integer count) {
        if (value < price) {
            return Triple.of(value, count, caps);
        }
        value = value - price + empty;
        count = count + 1;
        caps  = caps + 1;
        if (caps >= 2) {
            value = value + cap;
            caps  = caps - 2;
        }
        return calc(value, caps, count);
    }

    public static void main(String[] args) {
        System.out.println("----------10-----------");
        System.out.println(calc(10));
        System.out.println("----------11-----------");
        System.out.println(calc(11));
        System.out.println("----------15-----------");
        System.out.println(calc(15));
        System.out.println("----------16-----------");
        System.out.println(calc(16));
        System.out.println("----------17-----------");
        System.out.println(calc(17));
        System.out.println("----------20-----------");
        System.out.println(calc(20));
        System.out.println("----------21-----------");
        System.out.println(calc(21));
        System.out.println("----------24-----------");
        System.out.println(calc(24));
        System.out.println("----------25-----------");
        System.out.println(calc(25));
        System.out.println("----------26-----------");
        System.out.println(calc(26));
        System.out.println("----------27-----------");
        System.out.println(calc(27));
        System.out.println("----------28-----------");
        System.out.println(calc(28));
        System.out.println("----------29-----------");
        System.out.println(calc(29));
        System.out.println("----------30-----------");
        System.out.println(calc(30));
    }
}
