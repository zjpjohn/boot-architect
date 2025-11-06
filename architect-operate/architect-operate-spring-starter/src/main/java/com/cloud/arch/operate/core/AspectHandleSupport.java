package com.cloud.arch.operate.core;

import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class AspectHandleSupport {

    private final AsyncLogDispatcher logDispatcher;

    public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Object    result    = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed(joinPoint.getArgs());
        } catch (Throwable error) {
            throwable = error;
        }
        long       takenTime  = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        LogContext logContext = new LogContext(joinPoint, takenTime, throwable);
        logDispatcher.publish(logContext);
        if (throwable != null) {
            throw throwable;
        }
        return result;
    }

}
