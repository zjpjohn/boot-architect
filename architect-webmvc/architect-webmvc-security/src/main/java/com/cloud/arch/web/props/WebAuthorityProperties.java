package com.cloud.arch.web.props;

import com.cloud.arch.web.support.UriResourceAuthority;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@ConfigurationProperties(prefix = WebAuthorityProperties.PROPS_PREFIX)
public class WebAuthorityProperties {

    public static final String PROPS_PREFIX    = "com.cloud.web.security";
    public static final String DEFAULT_PATTERN = "/**";

    /**
     * 是否开启权限校验
     */
    private boolean      enable        = true;
    /**
     * 拦截请求url正则集合，多个正则以','分割
     */
    private String       patterns      = DEFAULT_PATTERN;
    /**
     * 不拦截请求url正则集合，多个以','分割
     */
    private String       excludes;
    /**
     * 是否开启权限校验缓存 开启-会对校验结果进行短时缓存 不开启-每一次都校验
     */
    private boolean      cached        = false;
    /**
     * 开启授权结果缓存后缓存最大容量
     */
    private Integer      cacheMaxSize  = 1024;
    /**
     * 授权结果缓存过期时间，单位分钟
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration     expireMinutes = Duration.ofMinutes(10);
    /**
     * 基于url资源的权限校验集合
     * 如果未配置则不启用权限拦截器
     * resource格式: uri路径 | *所有方法或请求方法集合 | *默认或用户访问域集合 | *默认或用户角色role(r1,r2,r3,...)或权限集合permit(p1,p2,p3,..)
     * uriPattern | *或者post,put,get,delete | *或者d1,d2,d3,... | *或者role(r1,r2,r3,...)或permit(p1,p2,p3,...)
     * 配置示例：
     * /user/** | post,put | * | permit(user:create) and(or) role(system)
     * /job/execute | post,put | system | permit(job:write)
     * /job/execute | get | system | permit(job:write,job:read)
     */
    private List<String> resources     = Lists.newArrayList();

    public List<UriResourceAuthority> parse() {
        return this.resources.stream().map(UriResourceAuthority::parse).toList();
    }

}
