package com.cloud.arch.web;

import java.util.Date;

/**
 * @param token    生成token内容
 * @param tokenId  当前token唯一标识
 * @param expireAt token到期截止时间
 */
public record TokenResult(String token, String tokenId, Date expireAt) {

}
