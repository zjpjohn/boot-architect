package com.cloud.arch.mybatis.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = MybatisFlexProperties.PROPS_PREFIX)
public class MybatisFlexProperties {

    public static final String PROPS_PREFIX = "mybatis-flex.extend";

    /**
     * 启用系统自定义主键
     */
    private KeyId key = new KeyId();

    @Data
    public static class KeyId {
        private boolean enabled = true;
        private String  name    = "keyId";
    }


}
