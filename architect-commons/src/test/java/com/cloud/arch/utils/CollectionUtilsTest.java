package com.cloud.arch.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CollectionUtils 集合工具类")
class CollectionUtilsTest {

    @Nested
    @DisplayName("空值检测")
    class Empty {

        @Test
        @DisplayName("null 集合判断为空")
        void shouldTreatNullCollectionAsEmpty() {
            assertThat(CollectionUtils.isEmpty((List<?>) null)).isTrue();
            assertThat(CollectionUtils.isNotEmpty((List<?>) null)).isFalse();
        }

        @Test
        @DisplayName("空集合判断为空")
        void shouldTreatEmptyCollectionAsEmpty() {
            assertThat(CollectionUtils.isEmpty(Collections.emptyList())).isTrue();
            assertThat(CollectionUtils.isNotEmpty(Collections.emptyList())).isFalse();
        }

        @Test
        @DisplayName("非空集合判断不为空")
        void shouldTreatNonEmptyCollectionAsNotEmpty() {
            assertThat(CollectionUtils.isEmpty(List.of(1))).isFalse();
            assertThat(CollectionUtils.isNotEmpty(List.of(1))).isTrue();
        }

        @Test
        @DisplayName("null Map 判断为空")
        void shouldTreatNullMapAsEmpty() {
            assertThat(CollectionUtils.isEmpty((Map<?, ?>) null)).isTrue();
            assertThat(CollectionUtils.isNotEmpty((Map<?, ?>) null)).isFalse();
        }

        @Test
        @DisplayName("空 Map 判断为空")
        void shouldTreatEmptyMapAsEmpty() {
            assertThat(CollectionUtils.isEmpty(Collections.emptyMap())).isTrue();
        }
    }

    @Nested
    @DisplayName("集合转换")
    class Convert {

        @Test
        @DisplayName("toList 转换集合元素")
        void shouldConvertToList() {
            List<String> result = CollectionUtils.toList(List.of(1, 2, 3), String::valueOf);
            assertThat(result).containsExactly("1", "2", "3");
        }

        @Test
        @DisplayName("toList null 入参返回空列表")
        void shouldReturnEmptyListForNullInput() {
            assertThat(CollectionUtils.toList(null, String::valueOf)).isEmpty();
        }

        @Test
        @DisplayName("distinctList 去重转换")
        void shouldDistinctConvert() {
            List<Integer> result = CollectionUtils.distinctList(List.of(1, 2, 2, 3), v -> v);
            assertThat(result).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("toSet 转换为去重集合")
        void shouldConvertToSet() {
            Set<Integer> result = CollectionUtils.toSet(List.of(1, 2, 2, 3), v -> v);
            assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
        }

        @Test
        @DisplayName("toMap 按 key Function 转换")
        void shouldConvertToMapByKeyFunction() {
            Map<String, String> result = CollectionUtils.toMap(List.of("a1", "b2", "c3"),
                                                               s -> s.substring(0, 1));
            assertThat(result).containsOnlyKeys("a", "b", "c");
        }

        @Test
        @DisplayName("toMap key 重复时后者覆盖前者")
        void shouldOverrideOnDuplicateKey() {
            Map<String, String> result = CollectionUtils.toMap(List.of("a1", "a2"),
                                                               s -> s.substring(0, 1));
            assertThat(result).hasSize(1).containsEntry("a", "a2");
        }
    }

    @Nested
    @DisplayName("平铺与分组")
    class FlatAndGroup {

        @Test
        @DisplayName("flatList 扁平化转换")
        void shouldFlatList() {
            List<List<Integer>> source = List.of(List.of(1, 2), List.of(3, 4));
            List<Integer> result = CollectionUtils.flatList(source, v -> v);
            assertThat(result).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("flatList null 入参返回空列表")
        void shouldReturnEmptyForFlatListNullInput() {
            assertThat(CollectionUtils.flatList(null, v -> List.of())).isEmpty();
        }

        @Test
        @DisplayName("groupBy 按分类器分组")
        void shouldGroupByClassifier() {
            Map<Integer, List<String>> result = CollectionUtils.groupBy(
                    List.of("a1", "a2", "b1"),
                    s -> s.charAt(0) - 'a');
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).containsExactly("a1", "a2");
            assertThat(result.get(1)).containsExactly("b1");
        }

        @Test
        @DisplayName("groupBy null 入参返回空 Map")
        void shouldReturnEmptyForGroupByNullInput() {
            assertThat(CollectionUtils.groupBy(null, v -> v)).isEmpty();
        }

        @Test
        @DisplayName("counting 分组统计计数")
        void shouldCountByClassifier() {
            Map<String, Long> result = CollectionUtils.counting(
                    List.of("a", "a", "b"),
                    s -> s);
            assertThat(result).containsEntry("a", 2L).containsEntry("b", 1L);
        }
    }

    @Nested
    @DisplayName("集合查找")
    class Search {

        @Test
        @DisplayName("contains 检测元素存在")
        void shouldFindElementInIterator() {
            assertThat(CollectionUtils.contains(List.of(1, 2, 3).iterator(), 2)).isTrue();
            assertThat(CollectionUtils.contains(List.of(1, 2, 3).iterator(), 4)).isFalse();
        }

        @Test
        @DisplayName("findFirstMatch 查找第一个交集元素")
        void shouldFindFirstCommonElement() {
            Object result = CollectionUtils.findFirstMatch(
                    List.of(1, 2, 3), List.of(3, 4, 5));
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("findFirstMatch 无交集返回 null")
        void shouldReturnNullIfNoCommonElement() {
            assertThat(CollectionUtils.findFirstMatch(
                    List.of(1, 2), List.of(3, 4))).isNull();
        }

        @Test
        @DisplayName("firstElement List")
        void shouldGetFirstElementOfList() {
            assertThat(CollectionUtils.firstElement(List.of(1, 2, 3))).isEqualTo(1);
            assertThat((Object) CollectionUtils.firstElement(Collections.emptyList())).isNull();
        }

        @Test
        @DisplayName("lastElement List")
        void shouldGetLastElementOfList() {
            assertThat(CollectionUtils.lastElement(List.of(1, 2, 3))).isEqualTo(3);
            assertThat((Object) CollectionUtils.lastElement(Collections.emptyList())).isNull();
        }
    }

    @Nested
    @DisplayName("Map 辅助")
    class MapHelpers {

        @Test
        @DisplayName("newHashMap 创建预期大小的 HashMap")
        void shouldCreateHashMapWithExpectedSize() {
            Map<String, Object> map = CollectionUtils.newHashMap(10);
            assertThat(map).isInstanceOf(HashMap.class).isEmpty();
        }

        @Test
        @DisplayName("toMultiValueMap 转换为多值 Map")
        void shouldConvertToMultiValueMap() {
            Map<String, List<String>> source = new HashMap<>();
            source.put("a", List.of("1", "2"));
            var result = CollectionUtils.toMultiValueMap(source);
            assertThat(result.get("a")).containsExactly("1", "2");
        }
    }
}
