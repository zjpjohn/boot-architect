package com.cloud.arch.transaction.core;

import com.cloud.arch.transaction.annotation.TxAsync;
import com.cloud.arch.transaction.utils.AsyncTxState;
import com.cloud.arch.utils.IdWorker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
@Getter
public class AsyncTxInvoker {

    private final Object                     target;
    private final Method                     method;
    private final TxAsync                    annotation;
    private final AsyncTxVersion             version;
    private final Class<?>                   targetClass;
    private final PlatformTransactionManager transactionManager;
    private final TransactionAttributeSource transactionAttributeSource;
    private       String                     key;
    private       int                        maxRetry;
    private       long                       retryInterval;

    public AsyncTxInvoker(Object target,
                          Method method,
                          TxAsync annotation,
                          PlatformTransactionManager transactionManager,
                          TransactionAttributeSource transactionAttributeSource) {
        this.annotation = annotation;
        // 事务管理器
        this.transactionManager = transactionManager;
        // 事务注解解析获取事务属性
        this.transactionAttributeSource = transactionAttributeSource;
        // 获取代理对象的原始对象,防止在执行方法是受其他aop的影响
        this.target = AopProxyUtils.getSingletonTarget(target);
        // 获取对象的类型
        this.targetClass = Objects.requireNonNull(this.target).getClass();
        // 获取执行方法
        this.method = AopUtils.getMostSpecificMethod(method, targetClass);
        //当前事务执行版本
        this.version = new AsyncTxVersion(annotation.version());
        // 初始化校验
        this.initializeAndCheck();
    }

    private void initializeAndCheck() {
        if (!method.getReturnType().equals(void.class)) {
            throw new IllegalArgumentException("异步任务方法返回值为void。");
        }
        this.retryInterval = annotation.retryInterval();
        if (this.retryInterval < 10) {
            throw new IllegalArgumentException("异步任务重试时间间隔必须大于10秒。");
        }
        this.maxRetry = annotation.maxRetry();
        if (this.maxRetry <= 0) {
            throw new IllegalArgumentException("异步任务重试次数必须大于0。");
        }
        this.key = asyncKey(targetClass, method, annotation.name());
    }

    /**
     * 构建异步任务事件
     *
     * @param arguments 异步方法参数
     */
    public AsyncTxEvent build(Object[] arguments) {
        AsyncTxEvent  txEvent = new AsyncTxEvent();
        AsyncTxParams params  = new AsyncTxParams(arguments);
        txEvent.setRetries(0);
        txEvent.setData(params);
        txEvent.setAsyncKey(this.key);
        txEvent.setId(IdWorker.nextId());
        txEvent.setMaxRetry(this.maxRetry);
        txEvent.setState(AsyncTxState.READY);
        txEvent.setVersion(version.getVersion());
        txEvent.setRetryInterval(this.retryInterval);
        txEvent.setShardKey(AsyncTxSharding.shardingKey());
        return txEvent;
    }

    /**
     * 重试执行方法
     *
     * @param event 异步事物事件
     */
    public void invokeRetry(AsyncTxEvent event) {
        AsyncTxVersion targetVersion = event.getEventVersion();
        if (!this.version.equals(targetVersion)) {
            log.warn("数据[{}]版本[{}]与执行器版本[{}]不一致.", event.getAsyncKey(), targetVersion.getVersion(), this.version.getVersion());
            return;
        }
        this.invoke(event.getData());
    }

    /**
     * 立即执行方法
     *
     * @param params 方法参数
     */
    public void invoke(AsyncTxParams params) {
        Object[]             arguments            = params.jsonArguments(method);
        TransactionAttribute transactionAttribute = this.transactionAttributeSource.getTransactionAttribute(this.method, this.targetClass);
        if (transactionAttribute == null) {
            invokeWithoutTx(arguments);
            return;
        }
        invokeWithTx(transactionAttribute, arguments);
    }

    private void invokeWithoutTx(Object[] arguments) {
        try {
            this.method.invoke(this.target, arguments);
        } catch (Exception error) {
            throw new RuntimeException(error.getMessage(), error);
        }
    }

    private void invokeWithTx(TransactionAttribute transactionAttribute, Object[] arguments) {
        TransactionStatus transaction = this.transactionManager.getTransaction(transactionAttribute);
        try {
            this.method.invoke(this.target, arguments);
            this.transactionManager.commit(transaction);
        } catch (Exception error) {
            this.transactionManager.rollback(transaction);
            throw new RuntimeException(error.getMessage(), error);
        }
    }

    /**
     * 解析异步任务key
     */
    public static String asyncKey(Class<?> target, Method method, String extra) {
        String key = target.getSimpleName() + "." + method.getName();
        if (StringUtils.isNotBlank(extra)) {
            key = key + "." + extra;
        }
        return key;
    }

}
