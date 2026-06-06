package com.cloud.arch.web.mask;

import org.apache.commons.lang3.StringUtils;

public class MaskUtils {

    public static String mask(String original, MaskType type, double ratio, char maskChar, int minLength) {
        if (StringUtils.isBlank(original)) {
            return original;
        }
        return switch (type) {
            case PASSWORD -> "********";
            case NAME -> maskName(original);
            case EMAIL -> maskEmail(original);
            case MOBILE -> maskMobile(original);
            case ID_CARD -> maskIdCard(original);
            case BANK_CARD -> maskBankCard(original);
            case CUSTOM -> maskByRatio(original, ratio, maskChar, minLength);
        };

    }

    private static String maskMobile(String original) {
        if (original.length() < 11) {
            return original;
        }
        return original.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    private static String maskBankCard(String original) {
        if (original.length() <= 8) {
            return original;
        }
        return original.replaceAll("(\\d{4})\\d+(\\d{4})", "$1****$2");
    }

    private static String maskIdCard(String original) {
        int length = original.length();
        if (length == 15) {
            return original.replaceAll("(\\d{4})\\d{7}(\\d{4})", "$1*******$2");
        }
        if (length == 18) {
            return original.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
        }
        return original;
    }

    private static String maskName(String original) {
        if (original.length() == 1) return "*";
        return original.charAt(0) + "**";
    }

    public static String maskEmail(String original) {
        int atIndex = original.indexOf('@');
        if (atIndex <= 0) {
            return original;
        }
        String prefix = original.substring(0, atIndex);
        String suffix = original.substring(atIndex);
        if (prefix.length() <= 2) {
            return prefix + "***" + suffix;
        }
        return prefix.substring(0, 2) + "***" + suffix;
    }

    private static String maskByRatio(String original, double ratio, char maskChar, int minMaskLen) {
        int len = original.length();
        if (len == 0) return original;

        int maskLen = (int) Math.floor(len * ratio);
        if (maskLen < minMaskLen && len > minMaskLen) {
            maskLen = Math.min(minMaskLen, len - 2);
        }
        if (maskLen >= len) {
            return String.valueOf(maskChar).repeat(len);
        }

        int keepTotal = len - maskLen;
        int keepLeft  = keepTotal / 2;
        int keepRight = keepTotal - keepLeft;

        String left   = original.substring(0, keepLeft);
        String right  = original.substring(len - keepRight);
        String masked = String.valueOf(maskChar).repeat(maskLen);
        return left + masked + right;
    }

}
