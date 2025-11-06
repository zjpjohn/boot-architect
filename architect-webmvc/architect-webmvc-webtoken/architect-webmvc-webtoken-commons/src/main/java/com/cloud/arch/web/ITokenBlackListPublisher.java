package com.cloud.arch.web;

import java.time.LocalDateTime;

public interface ITokenBlackListPublisher {

    /**
     * 发布token到黑名单中
     *
     * @param tokenId  token标识
     * @param expireAt token过期截止时间
     */
    void publish(String tokenId, LocalDateTime expireAt);

}
