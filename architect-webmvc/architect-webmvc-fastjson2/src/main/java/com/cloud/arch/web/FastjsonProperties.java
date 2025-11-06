package com.cloud.arch.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Data
@ConfigurationProperties(prefix = "com.cloud.web.fastjson")
public class FastjsonProperties {

    /**
     * 序列化编码，默认UTF-8
     */
    private Charset charset            = StandardCharsets.UTF_8;
    /**
     * 日期格式
     */
    private String  dateFormat         = "yyyy/MM/dd HH:mm:ss";
    /**
     * 是否写入内容长度
     */
    private boolean writeContentLength = true;
    /**
     * 是否启用jsonb
     */
    private boolean jsonb              = false;
    /**
     * 是否忽略控制字段
     */
    private boolean ignoreNull         = true;

}
