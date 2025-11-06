package com.cloud.arch.rocket.utils;

import java.util.zip.CRC32;


public class HashUtil {

    public static long crc32code(byte[] body) {
        CRC32 crc32 = new CRC32();
        crc32.update(body);
        return crc32.getValue();
    }
}
