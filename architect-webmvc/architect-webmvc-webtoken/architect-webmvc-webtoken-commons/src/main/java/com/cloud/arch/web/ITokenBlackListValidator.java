package com.cloud.arch.web;

public interface ITokenBlackListValidator {

    /**
     * 检查token是否在黑名单中
     *
     * @param tokenId token标识
     */
    boolean validate(String tokenId);

}
