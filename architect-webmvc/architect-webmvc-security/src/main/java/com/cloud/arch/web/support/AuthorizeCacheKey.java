package com.cloud.arch.web.support;

import org.springframework.context.expression.AnnotatedElementKey;

public record AuthorizeCacheKey(String domain, String identity, AnnotatedElementKey elementKey) {
}
