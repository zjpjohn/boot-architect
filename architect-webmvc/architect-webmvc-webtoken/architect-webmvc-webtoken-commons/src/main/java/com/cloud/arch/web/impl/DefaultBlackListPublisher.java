package com.cloud.arch.web.impl;

import com.cloud.arch.web.ITokenBlackListPublisher;

import java.time.LocalDateTime;

public class DefaultBlackListPublisher implements ITokenBlackListPublisher {

    @Override
    public void publish(String tokenId, LocalDateTime expireAt) {
    }
}
