package com.cloud.arch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IpRegionResult IP 地域结果")
class IpRegionResultTest {

    @Nested
    @DisplayName("字段赋值")
    class Fields {

        @Test
        @DisplayName("所有字段正确赋值")
        void shouldSetAllFields() {
            IpRegionResult result = new IpRegionResult();
            result.setIp("8.8.8.8");
            result.setCountry("美国");
            result.setProvince("加利福尼亚");
            result.setCity("洛杉矶");
            result.setRegion("北美");
            result.setIsp("Google");

            assertThat(result.getIp()).isEqualTo("8.8.8.8");
            assertThat(result.getCountry()).isEqualTo("美国");
            assertThat(result.getProvince()).isEqualTo("加利福尼亚");
            assertThat(result.getCity()).isEqualTo("洛杉矶");
            assertThat(result.getIsp()).isEqualTo("Google");
        }
    }

    @Nested
    @DisplayName("getAddress()")
    class GetAddress {

        @Test
        @DisplayName("所有字段非空 → 逗号拼接")
        void shouldJoinAllNonBlank() {
            IpRegionResult result = new IpRegionResult();
            result.setCountry("中国");
            result.setProvince("浙江");
            result.setCity("杭州");
            result.setRegion("华东");
            assertThat(result.getAddress()).isEqualTo("中国,浙江,杭州,华东");
        }

        @Test
        @DisplayName("部分字段为空 → 只拼接非空字段")
        void shouldSkipBlankFields() {
            IpRegionResult result = new IpRegionResult();
            result.setCountry("中国");
            result.setProvince("");
            result.setCity("杭州");
            result.setRegion(null);
            assertThat(result.getAddress()).isEqualTo("中国,杭州");
        }

        @Test
        @DisplayName("全部为空 → 空字符串")
        void shouldReturnEmptyWhenAllBlank() {
            IpRegionResult result = new IpRegionResult();
            assertThat(result.getAddress()).isEmpty();
        }
    }
}
