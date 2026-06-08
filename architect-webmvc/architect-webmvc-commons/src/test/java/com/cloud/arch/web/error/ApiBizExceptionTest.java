package com.cloud.arch.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("ApiBizException 业务异常")
class ApiBizExceptionTest {

    @Nested
    @DisplayName("构造函数")
    class Constructors {

        @Test
        @DisplayName("双参构造默认 status=OK, stacked=false, data=null")
        void shouldHaveDefaults() {
            ApiBizException ex = new ApiBizException(1001, "业务错误");
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.OK);
            assertThat(ex.getCode()).isEqualTo(1001);
            assertThat(ex.getError()).isEqualTo("业务错误");
            assertThat(ex.getData()).isNull();
            assertThat(ex.getMessage()).isEqualTo("业务错误");
        }

        @Test
        @DisplayName("三参构造可控制 stacked")
        void shouldControlStacked() {
            ApiBizException ex = new ApiBizException(1002, "错误", true);
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.OK);
            assertThat(ex.getCode()).isEqualTo(1002);
        }
    }

    @Nested
    @DisplayName("errReturn()")
    class ErrReturn {

        @Test
        @DisplayName("返回 ApiReturn 包含异常信息")
        void shouldCreateApiReturn() {
            ApiBizException ex = new ApiBizException(HttpStatus.BAD_REQUEST, 4001, "参数错误");
            assertThat(ex.errReturn())
                    .extracting(r -> r.getBody().error())
                    .isEqualTo("参数错误");
        }
    }

    @Nested
    @DisplayName("from(ErrorHandler)")
    class FromErrorHandler {

        @Test
        @DisplayName("从 ErrorHandler 构造异常")
        void shouldCreateFromHandler() {
            ErrorHandler handler = mock(ErrorHandler.class);
            when(handler.getStatus()).thenReturn(HttpStatus.FORBIDDEN);
            when(handler.getCode()).thenReturn(403);
            when(handler.getError()).thenReturn("forbidden");

            ApiBizException ex = ApiBizException.from(handler);
            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getCode()).isEqualTo(403);
            assertThat(ex.getError()).isEqualTo("forbidden");
        }
    }
}
