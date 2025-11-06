package com.cloud.arch.http;

import org.apache.hc.core5.http.HttpHost;

public class HttpProxyContext {

    private HttpProxyContext() {
        throw new UnsupportedOperationException("not support invoke.");
    }

    private static final ThreadLocal<HttpHost> proxyHolder = new ThreadLocal<>();

    public static void setProxy(HttpHost proxy) {
        proxyHolder.set(proxy);
    }

    public static HttpHost getProxy() {
        return proxyHolder.get();
    }

    public static void clear() {
        proxyHolder.remove();
    }
}
