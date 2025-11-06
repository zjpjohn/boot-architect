package com.cloud.arch.web.support;

import org.springframework.context.expression.AnnotatedElementKey;

public record AuthorizeCacheKey(String domain, String identity, AnnotatedElementKey elementKey) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizeCacheKey that = (AuthorizeCacheKey) o;
        return domain.equals(that.domain) && identity.equals(that.identity) && elementKey.equals(that.elementKey);
    }

}
