package com.cloud.arch.event.annotations;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Inherited
@IndexAnnotated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Subscribes {

    Subscribe[] value();

}
