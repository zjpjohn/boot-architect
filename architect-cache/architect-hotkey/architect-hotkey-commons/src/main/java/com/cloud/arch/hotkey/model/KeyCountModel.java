package com.cloud.arch.hotkey.model;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class KeyCountModel {

    /**
     * 规则名称
     * 如:cache(缓存名称)#**#timestamp(时间戳:2022-11-07 18:35:23)
     */
    private String ruleKey;
    /**
     * 总访问次数
     */
    private int    totalHitCount;
    /**
     * 成为热key后访问次数
     */
    private int    hotHitCount;
    /**
     * 发送时的时间戳
     */
    private long   createTime;

}
