package com.cloud.arch.cache.warmup.endpoint;

import com.cloud.arch.cache.warmup.core.WarmUpMeta;
import com.cloud.arch.cache.warmup.core.WarmUpResult;
import com.cloud.arch.cache.warmup.core.WarmUpTemplate;
import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.annotation.ApiBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存预热 REST 端点
 */
@ApiBody
@RestController
@RequestMapping("/actuator/warmup")
public class WarmUpEndpoint {

    private final WarmUpTemplate warmUpTemplate;

    public WarmUpEndpoint(WarmUpTemplate warmUpTemplate) {
        this.warmUpTemplate = warmUpTemplate;
    }

    /**
     * POST /actuator/warmup/cache/user_cache
     * Body（可选）: [["EAST", 1001], ["WEST", 1002]]
     * 无 body 则使用 YAML 配置的 args
     */
    @PostMapping(value = "/cache/{cacheName}")
    public CompletableFuture<WarmUpResult> warmUpCache(@PathVariable String cacheName,
                                                       @RequestBody(required = false) List<List<Object>> args) {
        if (CollectionUtils.isEmpty(args)) {
            return warmUpTemplate.warmUp(cacheName);
        }
        List<Object[]> argsList = args.stream().map(List::toArray).toList();
        return warmUpTemplate.warmUp(cacheName, argsList);
    }

    /**
     * 查询所有可预热缓存的元数据
     */
    @GetMapping(value = "/caches")
    public List<WarmUpMeta> getCaches() {
        return warmUpTemplate.metas();
    }

    /**
     * 查询单个缓存预热的元数据详情
     */
    @GetMapping(value = "/caches/{cacheName}")
    public WarmUpMeta getCache(@PathVariable String cacheName) {
        return warmUpTemplate.meta(cacheName);
    }
}
