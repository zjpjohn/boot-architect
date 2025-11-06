package com.boot.architect.application.command.executor;

import com.cloud.arch.transaction.annotation.TxAsync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StudentEditExecutor {

    @TxAsync
    @Transactional
    public void asyncAfter(Long id, String name) {
        try {
            log.info("编辑用户[{}],用户名称[{}]", id, name);
            Thread.sleep(1000L);
            log.info("编辑用户后进行耗时业务处理....");
            System.out.println(1 / 0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @TxAsync
    public void asyncNoArgs() {
        try {
            log.info("异步无参任务执行");
            Thread.sleep(1000L);
            log.info("编辑后执行异步无参数任务...");
            System.out.println(1 / 0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
