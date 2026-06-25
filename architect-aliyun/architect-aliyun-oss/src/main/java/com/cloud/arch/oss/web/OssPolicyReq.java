package com.cloud.arch.oss.web;

import lombok.Data;

@Data
public class OssPolicyReq {

    /**
     * 上传文件场景
     */
    private OssScene scene;
    /**
     * 文件名称
     */
    private String   fileName;
    /**
     * 上传用户标识
     */
    private Long     uploadBy;
    /**
     * 旧文件的链接
     */
    private String   replaceUrl;

}
