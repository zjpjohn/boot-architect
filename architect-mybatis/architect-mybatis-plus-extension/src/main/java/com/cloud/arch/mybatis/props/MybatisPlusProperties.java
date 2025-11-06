package com.cloud.arch.mybatis.props;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = MybatisPlusProperties.PROPS_PREFIX)
public class MybatisPlusProperties {

    public static final String PROPS_PREFIX = "com.cloud.mybatis-plus";

    /**
     * 填充创建时间时间字段名称
     */
    private String     gmtCreate  = "gmtCreate";
    /**
     * 填充更新时间字段
     */
    private String     gmtModify  = "gmtModify";
    /**
     * 是否启用自定义主键Id
     */
    private boolean    enableId   = true;
    /**
     * 是否启用系统乐观锁配置
     */
    private Version    version    = new Version();
    /**
     * 分页查询配置
     */
    private Pagination pagination = new Pagination();

    @Data
    public static class Version {

        private boolean enable = true;

    }

    @Data
    public static class Pagination {

        private DbType  type     = DbType.MYSQL;
        private Long    maxLimit = 500L;
        private boolean enable   = true;

    }

}
