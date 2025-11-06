package com.cloud.arch.web.props;

import com.cloud.arch.web.advice.ResponseEncryptor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = WebmvcProperties.PROPS_PREFIX)
public class WebmvcProperties {

    public static final String PROPS_PREFIX = "com.cloud.web";

    //是否启用扩展mvc
    private boolean       enable     = true;
    //字典数据配置
    private Dictionary    dictionary = new Dictionary();
    //接口加密配置
    private EncryptConfig encrypt    = new EncryptConfig();
    //接口版本控制
    private VersionConfig version    = new VersionConfig();

    @Data
    public static class Dictionary {
        /**
         * 是否暴露字典数据
         */
        private Boolean export   = false;
        /**
         * 暴露端点地址
         */
        private String  endpoint = "/dictionary";
    }

    @Data
    public static class EncryptConfig {
        /**
         * 响应加密密钥header
         */
        private String header  = ResponseEncryptor.DEFAULT_HEADER;
        /**
         * 加密模式:CBC,ECB
         */
        private String mode    = ResponseEncryptor.DEFAULT_MODE;
        /**
         * 加密填充类型:pkcs7,zero
         */
        private String padding = ResponseEncryptor.DEFAULT_PADDING;
    }

    @Data
    public static class VersionConfig {

        /**
         * 是否启用版本控制
         * 默认-未启用api版本
         */
        private boolean enable = false;
        /**
         * 是否使用header模式
         * true-header模式
         * false-param参数模式
         */
        private boolean header = true;
        /**
         * 使用版本控制的参数名称
         * 如；header模式设置header名称；params模式设置param参数名称
         */
        private String  name   = "";

    }

}
