package com.cloud.arch.operate.infrast.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.cloud.arch.operate.infrast.props.OperateProperties.PROPS_PREFIX;

@Data
@ConfigurationProperties(prefix = PROPS_PREFIX)
public class OperateProperties {

    public static final String PROPS_PREFIX = "com.cloud.operate";

    /**
     * 是否开启租户参数校验
     * 默认false
     */
    private Boolean tenantForce = false;

}
