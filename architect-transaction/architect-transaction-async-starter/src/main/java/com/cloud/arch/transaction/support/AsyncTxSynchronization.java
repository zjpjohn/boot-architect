package com.cloud.arch.transaction.support;

import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.cloud.arch.transaction.core.AsyncTxExecutor;
import com.cloud.arch.transaction.core.AsyncTxSharding;
import com.cloud.arch.transaction.core.IAsyncTxRepository;
import com.cloud.arch.utils.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AsyncTxSynchronization implements TransactionSynchronization {

    private final AsyncTxExecutor    asyncExecutor;
    private final IAsyncTxRepository asyncRepository;

    /**
     * 事务提交之前，当前方法仍在事务范围内,出现异常仍会回滚事务
     */
    @Override
    public void beforeCommit(boolean readOnly) {
        List<AsyncTxEvent> events = AsyncTxEventHolder.getEvents();
        if (CollectionUtils.isNotEmpty(events)) {
            asyncRepository.initialize(events);
        }
    }

    /**
     * 事务提交之后，当前方法不在事务范围内
     */
    @Override
    public void afterCommit() {
        List<AsyncTxEvent> events = AsyncTxEventHolder.getEvents();
        events.forEach(asyncExecutor::execute);
    }

    @Override
    public void afterCompletion(int status) {
        AsyncTxSharding.clear();
        AsyncTxEventHolder.clear();
    }
}
