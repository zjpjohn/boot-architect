package com.cloud.arch.web.support;

import com.cloud.arch.web.props.WebAuthorityProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class AuthorizeCacheManager {

    private Cache<AuthorizeCacheKey, GrantedResult> resultCache;

    public AuthorizeCacheManager(WebAuthorityProperties properties) {
        if (properties.isCached()) {
            this.resultCache = Caffeine.newBuilder()
                                       .maximumSize(properties.getCacheMaxSize())
                                       .expireAfterWrite(properties.getCacheExpire())
                                       .build();
        }
    }

    /**
     * 从缓存中查询授权结果
     *
     * @param cacheKey 缓存key
     */
    public GrantedResult fromCache(AuthorizeCacheKey cacheKey) {
        if (this.resultCache != null) {
            return this.resultCache.getIfPresent(cacheKey);
        }
        return null;
    }

    /**
     * 缓存授权结果
     *
     * @param cacheKey 缓存key
     * @param result   授权结果
     */
    public void cacheAuthorize(AuthorizeCacheKey cacheKey, GrantedResult result) {
        if (this.resultCache != null) {
            this.resultCache.put(cacheKey, result);
        }
    }

    /**
     * 清理指定用户标识已缓存的权限结果
     */
    public void invalidate(String identity) {
        if (resultCache != null) {
            this.resultCache.asMap().keySet().removeIf(key -> key.identity().equals(identity));
        }
    }

}
