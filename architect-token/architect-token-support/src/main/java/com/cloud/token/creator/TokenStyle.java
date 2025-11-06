package com.cloud.token.creator;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AllArgsConstructor
public enum TokenStyle {
    STYLE_UUID("uuid") {
        @Override
        public String generate() {
            return UUID.randomUUID().toString();
        }
    },
    STYLE_SIMPLE_UUID("simple-uuid") {
        @Override
        public String generate() {
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
    },
    STYLE_RANDOM_32("random-32") {
        @Override
        public String generate() {
            return randomString(32);
        }
    },
    STYLE_RANDOM_64("random-64") {
        @Override
        public String generate() {
            return randomString(64);
        }
    },
    STYLE_RANDOM_128("random-128") {
        @Override
        public String generate() {
            return randomString(128);
        }
    };

    public static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final String name;

    public abstract String generate();

    public static String randomString(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int number = ThreadLocalRandom.current().nextInt(62);
            sb.append(CHARS.charAt(number));
        }
        return sb.toString();
    }

    public static TokenStyle of(String value) {
        return Arrays.stream(values()).filter(e -> e.name.equals(value)).findFirst().orElse(STYLE_UUID);
    }

}
