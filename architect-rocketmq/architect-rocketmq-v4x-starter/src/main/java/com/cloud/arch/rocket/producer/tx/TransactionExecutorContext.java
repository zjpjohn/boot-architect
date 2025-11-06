package com.cloud.arch.rocket.producer.tx;

import org.aopalliance.intercept.MethodInvocation;

import java.util.Optional;

public class TransactionExecutorContext {

    private final static ThreadLocal<MethodInvocation> invocationHolder = new ThreadLocal<>();
    private final static ThreadLocal<InvokeResult>     resultHolder     = new ThreadLocal<>();

    /**
     * 将当前业务方法加入当前事务消息发送上下文中
     */
    public static void setInvocation(MethodInvocation invocation) {
        invocationHolder.set(invocation);
    }

    /**
     * 事物消息发送成功后回调执行业务方法
     */
    public static void execute() throws Throwable {
        MethodInvocation invocation = invocationHolder.get();
        if (invocation == null) {
            return;
        }
        try {
            Object result = invocation.proceed();
            resultHolder.set(new InvokeResult(result));
        } finally {
            invocationHolder.remove();
        }
    }

    /**
     * 获取当前上下文执行结果
     */
    public static Object getResult() {
        InvokeResult result = resultHolder.get();
        resultHolder.remove();
        return Optional.ofNullable(result)
                .map(InvokeResult::getResult)
                .orElse(null);
    }

    /**
     * 清空当前线程暂存数据，防止泄漏
     */
    public static void clear() {
        invocationHolder.remove();
        resultHolder.remove();
    }

    /**
     * 业务方法执行结果包装器
     */
    public static class InvokeResult {

        private final Object result;

        public InvokeResult(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }

}
