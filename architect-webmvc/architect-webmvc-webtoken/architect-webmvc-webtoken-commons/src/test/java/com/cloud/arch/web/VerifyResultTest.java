package com.cloud.arch.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VerifyResult 校验结果")
class VerifyResultTest {

    @Nested
    @DisplayName("构造函数和字段")
    class ConstructorAndFields {

        @Test
        @DisplayName("domain 参数正确赋值")
        void shouldSetDomain() {
            VerifyResult result = new VerifyResult("admin");
            assertThat(result.getDomain()).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("parameter()")
    class Parameters {

        @Test
        @DisplayName("追加参数到 parameters map")
        void shouldAddParameter() {
            VerifyResult result = new VerifyResult("domain");
            result.parameter("userId", 123L);
            assertThat(result.getParameters()).containsEntry("userId", 123L);
        }
    }

    @Nested
    @DisplayName("header()")
    class Headers {

        @Test
        @DisplayName("追加 header")
        void shouldAddHeader() {
            VerifyResult result = new VerifyResult("domain");
            result.header("Authorization", "Bearer xxx");
            assertThat(result.getHeaders()).containsEntry("Authorization", "Bearer xxx");
        }

        @Test
        @DisplayName("getIdentity() 返回 AUTH_IDENTITY_HEADER")
        void shouldGetIdentity() {
            VerifyResult result = new VerifyResult("domain");
            result.header(WebTokenConstants.AUTH_IDENTITY_HEADER, "user-1");
            assertThat(result.getIdentity()).isEqualTo("user-1");
        }
    }

    @Nested
    @DisplayName("addRetain()")
    class Retains {

        @Test
        @DisplayName("追加保留字段")
        void shouldAddRetain() {
            VerifyResult result = new VerifyResult("domain");
            result.addRetain("userId");
            result.addRetainAll(java.util.List.of("tenantId", "orgId"));
            assertThat(result.getRetains()).containsExactly("userId", "tenantId", "orgId");
        }
    }
}
