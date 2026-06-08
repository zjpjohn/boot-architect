package com.cloud.token.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.token.utils.TokenConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("Session 会话")
class SessionTest {

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("三参构造 → type/realm/loginId 正确")
        void shouldSetFieldsWithThreeArgConstructor() {
            Session session = new Session("web", "default", "user123");
            assertThat(session.getType()).isEqualTo("web");
            assertThat(session.getRealm()).isEqualTo("default");
            assertThat(session.getLoginId()).isEqualTo("user123");
            assertThat(session.getCreateTime()).isPositive();
        }

        @Test
        @DisplayName("四参构造 → 可指定 token")
        void shouldSetTokenWithFourArgConstructor() {
            Session session = new Session("app", "realm1", "user456", "token-abc");
            assertThat(session.getToken()).isEqualTo("token-abc");
        }
    }

    @Nested
    @DisplayName("getSessionId()")
    class GetSessionId {

        @Test
        @DisplayName("有 realm → prefix:realm:loginId")
        void shouldBuildWithRealm() {
            Session session = new Session("web", "myRealm", "user1");
            assertThat(session.getSessionId()).isEqualTo("s:myRealm:user1");
        }

        @Test
        @DisplayName("realm 为空 → prefix:loginId")
        void shouldBuildWithoutRealm() {
            Session session = new Session("web", "", "user1");
            assertThat(session.getSessionId()).isEqualTo("s:user1");
        }

        @Test
        @DisplayName("realm 为 null → prefix:loginId")
        void shouldBuildWithNullRealm() {
            Session session = new Session("web", null, "user1");
            assertThat(session.getSessionId()).isEqualTo("s:user1");
        }
    }

    @Nested
    @DisplayName("attr 管理")
    class AttributeManagement {

        @Test
        @DisplayName("appendAttr → 新增")
        void shouldAppendNewAttr() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            assertThat(session.getAttr("t1")).isNotNull();
        }

        @Test
        @DisplayName("appendAttr → 同 token 覆盖 device/attr")
        void shouldUpdateExistingAttr() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android", "old"));
            session.appendAttr(new TokenAttribute("t1", "ios", "new"));
            TokenAttribute attr = session.getAttr("t1");
            assertThat(attr.getDevice()).isEqualTo("ios");
            assertThat(attr.getAttr()).isEqualTo("new");
        }

        @Test
        @DisplayName("bindDevice → 创建新 attr")
        void shouldBindDevice() {
            Session session = new Session("web", "default", "user1");
            session.bindDevice("tokenX", "android");
            assertThat(session.getAttr("tokenX")).isNotNull();
            assertThat(session.getAttr("tokenX").getDevice()).isEqualTo("android");
        }

        @Test
        @DisplayName("getAttr → 不存在的 token 返回 null")
        void shouldReturnNullForMissingToken() {
            Session session = new Session("web", "default", "user1");
            assertThat(session.getAttr("nonexistent")).isNull();
        }

        @Test
        @DisplayName("removeAttr → 存在则删除并返回 true")
        void shouldRemoveExistingAttr() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            assertThat(session.removeAttr("t1")).isTrue();
            assertThat(session.getAttr("t1")).isNull();
        }

        @Test
        @DisplayName("removeAttr → 不存在返回 false")
        void shouldReturnFalseForMissingAttr() {
            Session session = new Session("web", "default", "user1");
            assertThat(session.removeAttr("nonexistent")).isFalse();
        }

        @Test
        @DisplayName("copyAttrs → 返回新列表（防御性拷贝）")
        void shouldReturnDefensiveCopy() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            List<TokenAttribute> copy = session.copyAttrs();
            copy.clear();
            assertThat(session.copyAttrs()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("attr 过滤")
    class AttributeFiltering {

        @Test
        @DisplayName("attrsByDevice → 匹配指定设备")
        void shouldFilterByDevice() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            session.appendAttr(new TokenAttribute("t2", "ios"));
            session.appendAttr(new TokenAttribute("t3", "android"));
            List<TokenAttribute> result = session.attrsByDevice("android");
            assertThat(result).hasSize(2);
            assertThat(result).extracting(TokenAttribute::getDevice).allMatch("android"::equals);
        }

        @Test
        @DisplayName("attrsByDevice → 空 device 返回全部")
        void shouldReturnAllWhenDeviceBlank() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            session.appendAttr(new TokenAttribute("t2", "ios"));
            assertThat(session.attrsByDevice("")).hasSize(2);
            assertThat(session.attrsByDevice(null)).hasSize(2);
        }

        @Test
        @DisplayName("tokensByDevice → 返回设备列表")
        void shouldReturnTokensByDevice() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            session.appendAttr(new TokenAttribute("t2", "ios"));
            session.appendAttr(new TokenAttribute("t3", "android"));
            List<String> tokens = session.tokensByDevice("android");
            assertThat(tokens).containsExactly("android", "android");
        }

        @Test
        @DisplayName("tokensByDevice → 空 device 返回所有")
        void shouldReturnAllTokensWhenDeviceBlank() {
            Session session = new Session("web", "default", "user1");
            session.appendAttr(new TokenAttribute("t1", "android"));
            session.appendAttr(new TokenAttribute("t2", "ios"));
            assertThat(session.tokensByDevice("")).hasSize(2);
        }
    }

    @Nested
    @DisplayName("extra 扩展数据")
    class Extra {

        @Test
        @DisplayName("putExtra → 存入")
        void shouldPutExtra() {
            Session session = new Session("web", "default", "user1");
            session.putExtra("key1", "value1");
            assertThat(session.getExtra("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("getExtra → 不存在返回 null")
        void shouldReturnNullForMissingExtra() {
            Session session = new Session("web", "default", "user1");
            assertThat(session.getExtra("nonexistent")).isNull();
        }

        @Test
        @DisplayName("putExtraIfAbsent → key 不存在时存入")
        void shouldPutIfAbsent() {
            Session session = new Session("web", "default", "user1");
            session.putExtraIfAbsent("key1", "value1");
            session.putExtraIfAbsent("key1", "value2");
            assertThat(session.getExtra("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("putExtraIfAbsent → 值为空字符串时覆盖")
        void shouldOverrideEmptyStringWithPutIfAbsent() {
            Session session = new Session("web", "default", "user1");
            session.putExtra("key1", "");
            session.putExtraIfAbsent("key1", "newValue");
            assertThat(session.getExtra("key1")).isEqualTo("newValue");
        }

        @Test
        @DisplayName("removeExtra → 存在则删除返回 true")
        void shouldRemoveExistingExtra() {
            Session session = new Session("web", "default", "user1");
            session.putExtra("key1", "value1");
            assertThat(session.removeExtra("key1")).isTrue();
            assertThat(session.getExtra("key1")).isNull();
        }

        @Test
        @DisplayName("removeExtra → 不存在返回 false")
        void shouldReturnFalseForMissingExtra() {
            Session session = new Session("web", "default", "user1");
            assertThat(session.removeExtra("nonexistent")).isFalse();
        }
    }
}
