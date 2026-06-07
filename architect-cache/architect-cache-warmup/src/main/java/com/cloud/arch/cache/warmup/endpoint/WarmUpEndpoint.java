package com.cloud.arch.cache.warmup.endpoint;

import com.cloud.arch.cache.warmup.core.WarmUpResult;
import com.cloud.arch.cache.warmup.core.WarmUpTemplate;
import com.cloud.arch.web.annotation.ApiBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @PostMapping(value = "/cache/{cacheName}", consumes = {"application/json", "*/*"})
    public WarmUpResult warmUpCache(@PathVariable String cacheName,
                                    @RequestBody(required = false) List<List<Object>> args) {
        if (args == null || args.isEmpty()) {
            return warmUpTemplate.warmUp(cacheName);
        }
        List<Object[]> argsList = args.stream()
                .map(List::toArray)
                .toList();
        return warmUpTemplate.warmUp(cacheName, argsList);
    }
}
