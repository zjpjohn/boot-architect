package com.cloud.arch.operate.aspect;

import com.cloud.arch.operate.core.AspectHandleSupport;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
@AllArgsConstructor
public class OperationLogAspect {

    private final AspectHandleSupport handleSupport;

    @Pointcut("@annotation(com.cloud.arch.operate.annotations.OpLog)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleSupport.invoke(joinPoint);
    }

}
