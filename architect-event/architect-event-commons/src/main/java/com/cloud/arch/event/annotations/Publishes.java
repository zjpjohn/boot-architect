package com.cloud.arch.event.annotations;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Documented
@IndexAnnotated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Publishes {

    Publish[] value();

}
