package com.cloud.arch.web.error;

import com.cloud.arch.web.utils.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.function.Supplier;

public interface ErrorHandler extends Supplier<ApiBizException> {

    /**
     * 错误响应状态
     */
    default HttpStatus getStatus() {
        return HttpStatus.OK;
    }

    /**
     * 错误相应状态码
     */
    default Integer getCode() {
        return this.getStatus().value();
    }

    /**
     * 错误相应消息内容
     */
    String getError();

    default ApiBizException get() {
        return ApiBizException.from(this);
    }

    /**
     * 目标对象校验
     * 1.支持传入布尔对象
     * 2.非布尔对象非空校验
     */
    default void check(Object value) {
        if (value instanceof Boolean target) {
            Assert.state(target, this);
            return;
        }
        if (value instanceof String target) {
            Assert.state(StringUtils.isNotBlank(target), this);
            return;
        }
        Assert.notNull(value, this);
    }

}
