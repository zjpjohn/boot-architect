package com.cloud.arch.web.utils;

import com.cloud.arch.web.error.ApiBizException;
import com.cloud.arch.web.error.ErrorHandler;

import java.io.Serializable;
import java.util.function.Supplier;

public class Assert {

    private Assert() {
        throw new UnsupportedOperationException("not support invoked construct.");
    }

    public static Supplier<ApiBizException> supply(ErrorHandler handler) {
        return () -> ApiBizException.from(handler);
    }

    public static Supplier<ApiBizException> supply(Serializable extra, ErrorHandler handler) {
        return () -> ApiBizException.from(handler, extra);
    }

    public static ApiBizException cast(ErrorHandler handler) {
        return ApiBizException.from(handler);
    }

    public static ApiBizException cast(Serializable extra, ErrorHandler handler) {
        return ApiBizException.from(handler, extra);
    }

    public static void throwError(ErrorHandler handler) {
        throw ApiBizException.from(handler);
    }

    public static void throwError(Serializable extra, ErrorHandler handler) {
        throw ApiBizException.from(handler, extra);
    }

    public static void state(boolean expression, Serializable data, ErrorHandler handler) {
        if (!expression) {
            throw ApiBizException.from(handler, data);
        }
    }

    public static void state(boolean expression, ErrorHandler handler) {
        if (!expression) {
            throw ApiBizException.from(handler);
        }
    }

    public static void state(boolean expression) {
        if (!expression) {
            throw new ApiBizException(400, "illegal params");
        }
    }

    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new ApiBizException(400, message);
        }
    }

    public static <T> T notNull(T reference, ErrorHandler handle) {
        if (reference == null) {
            throw ApiBizException.from(handle);
        }
        return reference;
    }

    public static <T> T notNull(T reference) {
        if (reference == null) {
            throw new ApiBizException(400, "data must not be null");
        }
        return reference;
    }

    public static <T> T notNull(T reference, String message) {
        if (reference == null) {
            throw new ApiBizException(400, message);
        }
        return reference;
    }

}
