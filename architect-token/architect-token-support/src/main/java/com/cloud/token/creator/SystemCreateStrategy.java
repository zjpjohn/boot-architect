package com.cloud.token.creator;

public class SystemCreateStrategy implements TokenCreateStrategy {

    private final TokenStyle tokenStyle;

    public SystemCreateStrategy(TokenStyle tokenStyle) {
        this.tokenStyle = tokenStyle;
    }

    @Override
    public String create(Object loginId, String realm) {
        return tokenStyle.generate();
    }

}
