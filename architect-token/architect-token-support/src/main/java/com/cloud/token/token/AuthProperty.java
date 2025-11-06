package com.cloud.token.token;

import com.cloud.token.utils.TokenConstants;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

@Data
@ToString
@Accessors(chain = true)
public class AuthProperty {

    /**
     * 登录设备类型
     */
    private String              device;
    /**
     * 登录token有效期
     */
    private long                timeout;
    /**
     * token最低活跃频率
     */
    private long                activeTimeout;
    /**
     * jwt模式下扩展信息
     */
    private Map<String, Object> payload;
    /**
     * 预定义token
     */
    private String              token;
    /**
     * 是否写入响应header
     */
    private boolean             writeHeader;
    /**
     * token附加数据
     */
    private Object              attr;

    public AuthProperty payload(String key, Object value) {
        if (this.payload == null) {
            this.payload = Maps.newLinkedHashMap();
        }
        this.payload.put(key, value);
        return this;
    }

    public Object payload(String key) {
        return Optional.ofNullable(this.payload).map(body -> body.get(key)).orElse(null);
    }

    public boolean hasPayload() {
        return this.payload != null && !this.payload.isEmpty();
    }

    public String getDevice() {
        return Optional.ofNullable(this.device).filter(StringUtils::isNotBlank).orElse(TokenConstants.DEFAULT_DEVICE);
    }

}
