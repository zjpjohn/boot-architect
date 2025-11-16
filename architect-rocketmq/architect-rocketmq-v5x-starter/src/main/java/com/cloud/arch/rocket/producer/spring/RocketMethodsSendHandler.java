package com.cloud.arch.rocket.producer.spring;


import com.cloud.arch.rocket.producer.core.RocketSendHandler;
import com.cloud.arch.rocket.utils.MethodUtils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public class RocketMethodsSendHandler implements InvocationHandler {

    private final Map<Method, RocketSendHandler> handlers;

    public RocketMethodsSendHandler(Map<Method, RocketSendHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        if (MethodUtils.isDefault(method)) {
            return invokeDefaultMethod(proxy, method, args);
        }
        return handlers.get(method).invoke(args);
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,
                                                                                                                int.class);
        constructor.setAccessible(true);
        final Class<?> declaringClass = method.getDeclaringClass();
        return constructor.newInstance(declaringClass,
                                       MethodHandles.Lookup.PRIVATE
                                               | MethodHandles.Lookup.PROTECTED
                                               | MethodHandles.Lookup.PACKAGE
                                               | MethodHandles.Lookup.PUBLIC)
                          .unreflectSpecial(method, declaringClass)
                          .bindTo(proxy)
                          .invokeWithArguments(args);
    }

}
