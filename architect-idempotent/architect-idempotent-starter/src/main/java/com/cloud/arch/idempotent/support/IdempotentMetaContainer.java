package com.cloud.arch.idempotent.support;

import com.cloud.arch.idempotent.annotation.Idempotent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotentMetaContainer {

    private final Map<AnnotatedElementKey, IdempotentMeta> metaCache = new ConcurrentHashMap<>(16);

    private final IdempotentParseEvaluator evaluator;

    public IdempotentMetaContainer() {
        this.evaluator = new IdempotentParseEvaluator();
    }

    public IdempotentInfo getIdempotent(IdempotentRootObject root) {
        Idempotent          annotation = root.getAnnotation();
        IdempotentMeta      meta       = getIdempotentMeta(annotation, root.method(), root.targetClass());
        AnnotatedElementKey elementKey = new AnnotatedElementKey(root.method(), root.targetClass());
        EvaluationContext   context    = evaluator.getContext(root);
        return meta.getIdempotent(elementKey, context, evaluator);
    }

    public IdempotentMeta getIdempotentMeta(Idempotent annotation, Method method, Class<?> targetClass) {
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);
        return this.metaCache.computeIfAbsent(elementKey, key -> new IdempotentMeta(annotation));
    }

}
