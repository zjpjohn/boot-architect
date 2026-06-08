package com.cloud.arch.cache.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CacheSettings / LocalCacheSettings 缓存配置")
class CacheSettingsTest {

    @Nested
    @DisplayName("LocalCacheSettings")
    class Local {

        @Test
        @DisplayName("构建器创建 LocalCacheSettings")
        void shouldBuildWithExpireMode() {
            LocalCacheSettings settings = LocalCacheSettings.builder()
                    .initialSize(100)
                    .maximumSize(500)
                    .expireTime(60)
                    .expireMode(ExpireMode.WRITE)
                    .build();

            assertThat(settings.getExpireMode()).isEqualTo(ExpireMode.WRITE);
            assertThat(settings.getMaximumSize()).isEqualTo(500);
            assertThat(settings.getInitialSize()).isEqualTo(100);
            assertThat(settings.getExpireTime()).isEqualTo(60);
        }

        @Test
        @DisplayName("ACCESS 过期模式")
        void shouldBuildWithAccessExpireMode() {
            LocalCacheSettings settings = LocalCacheSettings.builder()
                    .expireMode(ExpireMode.ACCESS)
                    .build();

            assertThat(settings.getExpireMode()).isEqualTo(ExpireMode.ACCESS);
        }

        @Test
        @DisplayName("toString 包含关键字段")
        void shouldContainKeyFieldsInToString() {
            LocalCacheSettings settings = LocalCacheSettings.builder()
                    .maximumSize(100)
                    .expireMode(ExpireMode.WRITE)
                    .build();

            assertThat(settings.toString()).contains("maximumSize=100", "WRITE");
        }
    }

    @Nested
    @DisplayName("CacheSettings")
    class Settings {

        @Test
        @DisplayName("构建器创建 CacheSettings")
        void shouldBuildCacheSettings() {
            LocalCacheSettings local = LocalCacheSettings.builder()
                    .maximumSize(200)
                    .build();

            CacheSettings settings = CacheSettings.builder()
                    .expire(300)
                    .enableLocal(true)
                    .allowNullValue(true)
                    .magnification(10)
                    .enableRefresh(false)
                    .local(local)
                    .build();

            assertThat(settings.getExpire()).isEqualTo(300);
            assertThat(settings.isAllowNullValue()).isTrue();
            assertThat(settings.isEnableLocal()).isTrue();
        }

        @Test
        @DisplayName("默认不允许 null 值")
        void shouldDefaultAllowNullToFalse() {
            CacheSettings settings = CacheSettings.builder().build();
            assertThat(settings.isAllowNullValue()).isFalse();
        }

        @Test
        @DisplayName("toString 包含所有关键字段")
        void shouldContainKeyFieldsInToString() {
            CacheSettings settings = CacheSettings.builder()
                    .expire(100)
                    .allowNullValue(true)
                    .enableLocal(false)
                    .build();

            assertThat(settings.toString())
                    .contains("expire=100", "allowNullValue=true", "enableLocal=false");
        }
    }
}
