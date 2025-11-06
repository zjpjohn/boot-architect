package com.cloud.token.creator;

@FunctionalInterface
public interface TokenCreateStrategy {

    /**
     * token生成策略
     *
     * @param loginId 账户标识
     * @param realm   账户领域体系标识
     */
    String create(Object loginId, String realm);

}
