package com.cloud.arch.transaction.support;

import com.cloud.arch.transaction.core.AsyncTxEvent;
import lombok.experimental.UtilityClass;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;

@UtilityClass
public class AsyncTxEventHolder {

    /**
     * 单签线程持有异步事件集合
     */
    private static final ThreadLocal<List<AsyncTxEvent>> eventHolder     = ThreadLocal.withInitial(LinkedList::new);
    /**
     * 当前线程是否注册事务同步器
     */
    private static final ThreadLocal<Boolean>            synchronization = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 发布异步事件
     */
    public static void publish(AsyncTxEvent event) {
        if (!synchronization.get()) {
            boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            Assert.state(transactionActive, "异步事件未处于事务中，请配置spring事务.");
            AsyncTxSynchronization asyncSynchronization = ApplicationContextHolder.getBean(AsyncTxSynchronization.class);
            TransactionSynchronizationManager.registerSynchronization(asyncSynchronization);
            synchronization.set(Boolean.TRUE);
        }
        eventHolder.get().add(event);
    }

    public static List<AsyncTxEvent> getEvents() {
        return eventHolder.get();
    }

    public static void clear() {
        synchronization.remove();
        eventHolder.remove();
    }

}
