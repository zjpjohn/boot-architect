package com.cloud.token.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenAttribute {

    /**
     * token值
     */
    private String token;
    /**
     * 当前token设备
     */
    private String device;
    /**
     * 当前token额外属性
     */
    private Object attr;

    public TokenAttribute(String token, String device) {
        this.token  = token;
        this.device = device;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TokenAttribute that = (TokenAttribute) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
