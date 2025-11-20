package com.cloud.arch.operate.core;

public class DefaultTenantResolver implements ITenantResolver {

    @Override
    public String resolve() {
        return "0";
    }

}
