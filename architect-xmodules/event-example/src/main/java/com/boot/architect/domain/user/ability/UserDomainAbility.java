package com.boot.architect.domain.user.ability;

import com.alibaba.fastjson2.JSON;
import com.boot.architect.domain.user.event.UserCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserDomainAbility {

    @EventListener
    public void createListener(UserCreateEvent event) {
        log.info("用户创建事件:{}", JSON.toJSONString(event));
    }

}
