package com.boot.architect.infrast.repository.repository;

import com.alibaba.fastjson2.JSON;
import com.boot.architect.domain.user.model.UserInfoDo;
import com.boot.architect.domain.user.repository.IUserRepository;
import com.cloud.arch.aggregate.Aggregate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class UserRepository implements IUserRepository {

    @Override
    public void save(Aggregate<Long, UserInfoDo> aggregate) {
        log.info("create user info:{}", JSON.toJSONString(aggregate.getRoot()));
    }

    @Override
    public Optional<Aggregate<Long, UserInfoDo>> ofNullable(Long id) {
        return IUserRepository.super.ofNullable(id);
    }

    @Override
    public Aggregate<Long, UserInfoDo> of(Long id) {
        return IUserRepository.super.of(id);
    }

}
