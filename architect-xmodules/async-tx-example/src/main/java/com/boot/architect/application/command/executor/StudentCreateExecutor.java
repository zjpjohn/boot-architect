package com.boot.architect.application.command.executor;

import com.cloud.arch.transaction.annotation.TxAsync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StudentCreateExecutor {

    @TxAsync
    @Transactional
    public void asyncAfter(Long id, String name) {
        try {
            log.info("创建用户[{}],用户名称[{}]", id, name);
            Thread.sleep(1000L);
            log.info("创建用户后进行耗时业务处理....");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
