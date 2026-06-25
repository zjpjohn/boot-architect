package com.cloud.arch.oss.web;

import com.cloud.arch.enums.Value;

public interface OssScene extends Value<String> {

    /**
     * 所属模块
     */
    String module();

    /**
     * 文件大小限制
     */
    long maxSize();

}
