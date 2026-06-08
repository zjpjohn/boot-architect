package com.cloud.arch.mutex.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MutexOwner 互斥锁持有者")
class MutexOwnerTest {

    @Nested
    @DisplayName("NONE 哨兵")
    class NoneSentinel {

        @Test
        @DisplayName("NONE 的 ownerId 为空字符串")
        void shouldHaveEmptyOwnerId() {
            assertThat(MutexOwner.NONE.getOwnerId()).isEmpty();
        }

        @Test
        @DisplayName("NONE 不是任何 contender 的持有者")
        void shouldNotBeOwnerOfAnyContender() {
            assertThat(MutexOwner.NONE.isOwner("any")).isFalse();
        }

        @Test
        @DisplayName("NONE 不在 TTL 内")
        void shouldNotBeInTtl() {
            assertThat(MutexOwner.NONE.isInTtl()).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 检测")
    class Ttl {

        @Test
        @DisplayName("ttlAt 在未来 → isInTtl 为 true")
        void shouldBeInTtlWhenTtlInFuture() {
            long futureTtl = System.currentTimeMillis() + 30_000;
            MutexOwner owner = new MutexOwner("node1",
                    System.currentTimeMillis(), futureTtl, futureTtl);
            assertThat(owner.isInTtl()).isTrue();
        }

        @Test
        @DisplayName("ttlAt 在过去 → isInTtl 为 false")
        void shouldNotBeInTtlWhenTtlInPast() {
            long pastTtl = System.currentTimeMillis() - 10_000;
            MutexOwner owner = new MutexOwner("node1",
                    System.currentTimeMillis() - 20_000, pastTtl, pastTtl);
            assertThat(owner.isInTtl()).isFalse();
        }

        @Test
        @DisplayName("isInTtl(contenderId) 同时校验持有者和 TTL")
        void shouldCheckOwnerAndTtl() {
            long futureTtl = System.currentTimeMillis() + 30_000;
            MutexOwner owner = new MutexOwner("node1",
                    System.currentTimeMillis(), futureTtl, futureTtl);
            assertThat(owner.isInTtl("node1")).isTrue();
            assertThat(owner.isInTtl("node2")).isFalse();
        }
    }

    @Nested
    @DisplayName("字符串构造")
    class StringConstructor {

        @Test
        @DisplayName("单参构造默认 ttlAt 和 transitionAt 为 Long.MAX_VALUE")
        void shouldSetMaxTtlAndTransition() {
            MutexOwner owner = new MutexOwner("node1");
            assertThat(owner.getOwnerId()).isEqualTo("node1");
            assertThat(owner.getTtlAt()).isEqualTo(Long.MAX_VALUE);
            assertThat(owner.getTransitionAt()).isEqualTo(Long.MAX_VALUE);
        }
    }
}
