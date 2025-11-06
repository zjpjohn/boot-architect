package com.cloud.arch.cache.interceptor.operation;

import com.cloud.arch.cache.annotations.CacheAction;
import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public abstract class AbsCacheOperation<T extends Annotation> {

    private final T           annotation;
    private final boolean     allowNullValue;
    private       String      name;
    private       Set<String> cacheNames;
    private       String      key;
    private       String      keyGenerator;
    private       String      cacheResolver;
    private       String      condition;

    public AbsCacheOperation(Method method, T annotation, CacheAction cacheAction) {
        this(method, false, annotation, cacheAction);
    }

    public AbsCacheOperation(Method method, boolean allowNullValue, T annotation, CacheAction cacheAction) {
        this.annotation     = annotation;
        this.allowNullValue = allowNullValue;
        this.setName(method.toString());
        this.build(method, annotation, cacheAction);
    }

    public boolean canBuildCache() {
        return false;
    }

    abstract void parse(Method method, T annotation);

    /**
     * 构建缓存操作信息
     *
     * @param method      缓存方法
     * @param annotation  方法缓存注解
     * @param cacheAction 类上缓存操作注解
     */
    private void build(Method method, T annotation, CacheAction cacheAction) {
        //解析具体缓存操作信息
        parse(method, annotation);
        //解析类上缓存操作设置为默认配置
        parseCacheAction(cacheAction);
        //校验缓存操作合法性
        validateOperation();
    }

    private void parseCacheAction(CacheAction action) {
        if (action != null) {
            String[] cacheNames = action.names();
            if (this.cacheNames.isEmpty() && cacheNames.length > 0) {
                this.setCacheNames(cacheNames);
            }
            String keyGenerator = action.keyGenerator();
            if (!StringUtils.hasText(this.key)
                && !StringUtils.hasText(this.keyGenerator)
                && StringUtils.hasText(keyGenerator)) {
                this.setKeyGenerator(keyGenerator);
            }
            String cacheResolver = action.cacheResolver();
            if (!StringUtils.hasText(this.cacheResolver) && StringUtils.hasText(cacheResolver)) {
                this.setCacheResolver(cacheResolver);
            }
        }
    }

    private void validateOperation() {
        if (StringUtils.hasText(key) && StringUtils.hasText(keyGenerator)) {
            throw new IllegalArgumentException("Invalid cache annotation on 'key' and 'keyGenerator' attribute, these attributes are mutually.");
        }
    }

    public void setName(String name) {
        Assert.hasText(name, "name must not be empty.");
        this.name = name;
    }

    public void setCacheNames(String... cacheNames) {
        this.cacheNames = new LinkedHashSet<>();
        for (String cacheName : cacheNames) {
            Assert.hasText(cacheName, "cache name must not be empty.");
            this.cacheNames.add(cacheName);
        }
    }

    public void setKey(String key) {
        Assert.notNull(key, "cache key must not be empty.");
        this.key = key;
    }

    public void setKeyGenerator(String keyGenerator) {
        Assert.notNull(keyGenerator, "key generator must not be empty.");
        this.keyGenerator = keyGenerator;
    }

    public void setCacheResolver(String cacheResolver) {
        Assert.notNull(cacheResolver, "cache resolver must not be empty.");
        this.cacheResolver = cacheResolver;
    }

    public void setCondition(String condition) {
        Assert.notNull(condition, "cache key condition must not be empty.");
        this.condition = condition;
    }

}
