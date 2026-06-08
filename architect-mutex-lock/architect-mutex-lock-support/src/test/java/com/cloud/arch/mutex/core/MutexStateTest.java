package com.cloud.arch.mutex.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MutexState 互斥锁状态")
class MutexStateTest {

    private final MutexOwner owner1 = new MutexOwner("node1");
    private final MutexOwner owner2 = new MutexOwner("node2");

    @Nested
    @DisplayName("NONE 哨兵")
    class NoneSentinel {

        @Test
        @DisplayName("NONE 的 before/after 均为 MutexOwner.NONE")
        void shouldHaveNoneBeforeAndAfter() {
            assertThat(MutexState.NONE.getBefore()).isSameAs(MutexOwner.NONE);
            assertThat(MutexState.NONE.getAfter()).isSameAs(MutexOwner.NONE);
        }

        @Test
        @DisplayName("NONE 状态未变化")
        void shouldNotBeChanged() {
            assertThat(MutexState.NONE.isChanged()).isFalse();
        }
    }

    @Nested
    @DisplayName("状态变更检测")
    class ChangeDetection {

        @Test
        @DisplayName("before 和 after 不同 ownerId → isChanged 为 true")
        void shouldDetectChange() {
            MutexState state = new MutexState(MutexOwner.NONE, owner1);
            assertThat(state.isChanged()).isTrue();
        }

        @Test
        @DisplayName("before 和 after 相同 ownerId → isChanged 为 false")
        void shouldNotDetectChangeWhenSameOwner() {
            MutexState state = new MutexState(owner1, owner1);
            assertThat(state.isChanged()).isFalse();
        }

        @Test
        @DisplayName("从 NONE 变为 owner1 → isAcquired(node1) 为 true")
        void shouldDetectAcquired() {
            MutexState state = new MutexState(MutexOwner.NONE, owner1);
            assertThat(state.isAcquired("node1")).isTrue();
            assertThat(state.isAcquired("node2")).isFalse();
        }

        @Test
        @DisplayName("从 owner1 变为 NONE → isReleased(node1) 为 true")
        void shouldDetectReleased() {
            MutexState state = new MutexState(owner1, MutexOwner.NONE);
            assertThat(state.isReleased("node1")).isTrue();
            assertThat(state.isReleased("node2")).isFalse();
        }
    }
}
