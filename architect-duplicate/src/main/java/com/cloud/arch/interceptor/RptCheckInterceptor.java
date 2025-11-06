package com.cloud.arch.interceptor;

import com.cloud.arch.support.RptCheckRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class RptCheckInterceptor {

    private final RptCheckRepository checkRepository;

    public RptCheckInterceptor(RptCheckRepository checkRepository) {
        this.checkRepository = checkRepository;
    }

    @Pointcut("@annotation(com.cloud.arch.annotation.RptCheck)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] arguments = joinPoint.getArgs();
        for (Object value : arguments) {
            checkRepository.check(value);
        }
        return joinPoint.proceed(arguments);
    }

}
