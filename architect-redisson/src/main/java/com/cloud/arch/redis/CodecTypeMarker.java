package com.cloud.arch.redis;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Documented
@IndexAnnotated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodecTypeMarker {
}
