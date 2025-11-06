package com.cloud.arch.oss.store;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OssResult {
    //oss对象名称
    private String  key;
    //oss对象uri
    private String  url;
    //存储对象类型:1-图片、2-视频
    private Integer type;
}
