package com.cloud.arch.hotkey.config.props;

import com.cloud.arch.hotkey.utils.WorkerConstants;
import com.google.common.base.Splitter;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
public class EtcdServerProperties {

    /**
     * etcd服务连接地址，集群地址用逗号分隔
     */
    private String  server;
    /**
     * 用户名
     */
    private String  name;
    /**
     * 授权密码
     */
    private String  password;
    /**
     * 连接超时时间
     */
    private Long    connectTimeout = 10000L;
    /**
     * 会话超时时间
     */
    private Integer sessionTimeout = 20;

    /**
     * 获取server地址集合
     */
    public List<String> getServers() {
        if (StringUtils.isBlank(this.server)) {
            throw new IllegalArgumentException("etcd server地址不允许为空.");
        }
        return Splitter.on(WorkerConstants.SERVER_DELIMITER).omitEmptyStrings().trimResults().splitToList(this.server);
    }

}
