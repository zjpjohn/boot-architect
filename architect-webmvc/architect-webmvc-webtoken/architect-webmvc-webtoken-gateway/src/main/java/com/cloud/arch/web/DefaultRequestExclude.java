package com.cloud.arch.web;

public class DefaultRequestExclude implements IAuthRequestExclude {

    @Override
    public boolean isExclude(String requestUri, String method) {
        return false;
    }

}
