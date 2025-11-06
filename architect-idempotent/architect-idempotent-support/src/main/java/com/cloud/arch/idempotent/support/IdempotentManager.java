package com.cloud.arch.idempotent.support;

import com.cloud.arch.web.error.ApiBizException;
import org.springframework.http.HttpStatus;

public interface IdempotentManager {

    /**
     * 尝试获取锁
     */
    boolean tryAcquire(IdempotentInfo idempotent);

    /**
     * 获取锁失败回调处理
     */
    default Object acquireFail(IdempotentInfo idempotent) {
        throw new ApiBizException(HttpStatus.BAD_REQUEST, 400, idempotent.message());
    }

    /**
     * 获取锁成功后，完成操作
     */
    void completed(IdempotentInfo idempotent, Throwable throwable);

}
