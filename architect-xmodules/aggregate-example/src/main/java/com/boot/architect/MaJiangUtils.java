package com.boot.architect;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.encrypt.AESKit;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class MaJiangUtils {

    public static void main(String[] args) {
        String          hex      = "20-00-05-10-00-40-00-18-4B-3F-00-00-AE-03-00-00-1A-E2-D2-1B-25-13-00-00-DE-E3-BC-36-00-00-00-00";
        String[]        hexArray = hex.split("-");
        List<Character> chars    = Lists.newArrayList();
        for (String s : hexArray) {
            char anInt = (char) Integer.parseInt(s, 16);
            chars.add(anInt);
        }
        System.out.println(JSON.toJSONString(chars));
    }

}
