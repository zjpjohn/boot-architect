package com.cloud.arch.event.reparation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReparationResponse 修复响应")
class ReparationResponseTest {

    @Nested
    @DisplayName("isSuccess()")
    class IsSuccess {

        @Test
        @DisplayName("code 为 200 → true")
        void shouldBeSuccess() {
            ReparationResponse resp = new ReparationResponse(200, "ok");
            assertThat(resp.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("code 非 200 → false")
        void shouldNotBeSuccess() {
            ReparationResponse resp = new ReparationResponse(500, "error");
            assertThat(resp.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("success() → code=200")
        void shouldCreateSuccess() {
            ReparationResponse resp = ReparationResponse.success(1L, "ok");
            assertThat(resp.getEventId()).isEqualTo(1L);
            assertThat(resp.getCode()).isEqualTo(200);
            assertThat(resp.getMessage()).isEqualTo("ok");
        }

        @Test
        @DisplayName("unAuthorized() → code=401")
        void shouldCreateUnauthorized() {
            ReparationResponse resp = ReparationResponse.unAuthorized("denied");
            assertThat(resp.getCode()).isEqualTo(401);
            assertThat(resp.getMessage()).isEqualTo("denied");
        }

        @Test
        @DisplayName("error() → code=500")
        void shouldCreateError() {
            ReparationResponse resp = ReparationResponse.error(1L, "boom");
            assertThat(resp.getCode()).isEqualTo(500);
            assertThat(resp.getEventId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("methodError() → code=405")
        void shouldCreateMethodError() {
            ReparationResponse resp = ReparationResponse.methodError("not allowed");
            assertThat(resp.getCode()).isEqualTo(405);
        }

        @Test
        @DisplayName("notFound() → code=401")
        void shouldCreateNotFound() {
            ReparationResponse resp = ReparationResponse.notFound("missing");
            assertThat(resp.getCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("badRequest() → code=400")
        void shouldCreateBadRequest() {
            ReparationResponse resp = ReparationResponse.badRequest(1L, "invalid");
            assertThat(resp.getCode()).isEqualTo(400);
            assertThat(resp.getEventId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("netError() → code=500")
        void shouldCreateNetError() {
            ReparationResponse resp = ReparationResponse.netError("timeout");
            assertThat(resp.getCode()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("双参构造函数")
    class TwoArgConstructor {

        @Test
        @DisplayName("code 和 message 正确赋值")
        void shouldSetCodeAndMessage() {
            ReparationResponse resp = new ReparationResponse(302, "redirect");
            assertThat(resp.getCode()).isEqualTo(302);
            assertThat(resp.getMessage()).isEqualTo("redirect");
        }
    }
}
