package com.cloud.arch.web.impl;

import com.cloud.arch.web.ITokenBlackListValidator;

public class DefaultBlackListValidator implements ITokenBlackListValidator {

    @Override
    public boolean validate(String tokenId) {
        return false;
    }

}
