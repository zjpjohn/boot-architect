package com.cloud.arch.hotkey.rule;

import lombok.*;

import java.util.Objects;

/**
 * 判断key成为热key的规则
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class KeyRule {

    /**
     * 应用名称
     */
    private String app;
    /**
     * 缓存名称
     */
    private String cache;
    /**
     * 间隔时间(秒)
     */
    private int    interval;
    /**
     * 累计数量阈值
     */
    private int    threshold;
    /**
     * 热key本地缓存时间(秒)
     */
    private int    duration;
    /**
     * 热key缓存值最小容量
     */
    private int    minimum;
    /**
     * 热key缓存值最大容量
     */
    private int    maximum;
    /**
     * 描述
     */
    private String desc;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KeyRule keyRule = (KeyRule) o;
        return app.equals(keyRule.app) && cache.equals(keyRule.cache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(app, cache);
    }
}
