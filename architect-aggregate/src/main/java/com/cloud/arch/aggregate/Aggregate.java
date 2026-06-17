package com.cloud.arch.aggregate;

import com.cloud.arch.aggregate.reflection.DeepEquals;
import com.cloud.arch.aggregate.reflection.ReflectionUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Aggregate<K extends Serializable, R extends AggregateRoot<K>> {

    /**
     * 获取聚合根数据
     */
    @Getter
    private final R                root;
    /**
     * 获取聚合根历史快照数据
     */
    @Getter
    private final R                snapshot;
    /**
     * 聚合实体对象类型
     */
    private final Class<R>         targetClass;
    /**
     * 聚合根仓储
     */
    private       Repository<K, R> repository;

    @SuppressWarnings("unchecked")
    public Aggregate(R root, DeepCopier copier) {
        Preconditions.checkNotNull(root, "聚合根对象不允许为空.");
        this.root = root;
        this.targetClass = (Class<R>) root.getClass();
        this.snapshot = copier.copy(root);
    }

    public Aggregate(R root, DeepCopier copier, Repository<K, R> repository) {
        this(root, copier);
        this.repository = repository;
    }

    /**
     * 聚合根处理 用于lambda链式操作
     *
     * @param consumer 处理lambda函数
     */
    public Aggregate<K, R> peek(Consumer<R> consumer) {
        consumer.accept(this.root);
        return this;
    }

    /**
     * 聚合根对象转换 用于lambda链式操作
     *
     * @param action 转换lambda
     */
    public <T> T map(Function<R, T> action) {
        return action.apply(this.root);
    }

    /**
     * 聚合根保存 用于lambda链式操作
     *
     * @param consumer 保存处理lambda
     */
    public Aggregate<K, R> save(Consumer<Aggregate<K, R>> consumer) {
        if (consumer != null) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * 根据持有的仓储持久化聚合根
     */
    public Aggregate<K, R> save() {
        if (this.repository != null) {
            this.repository.save(this);
        }
        return this;
    }

    /**
     * 聚合是否变更
     */
    public boolean hasChanged() {
        return !DeepEquals.deepEquals(root, snapshot);
    }

    /**
     * 判断是否为新创建的聚合
     */
    public boolean isNew() {
        return root.isNew();
    }

    /**
     * 获取更新数据后的聚合根实例
     */
    public R changed() {
        return changed(null);
    }

    public Optional<R> ifChanged() {
        return Optional.ofNullable(changed());
    }

    public Optional<R> ifChanged(String group) {
        return Optional.ofNullable(changed(group));
    }

    /**
     * 获取分组情况下更新数据后的聚合实例，仅包含变更字段。
     *
     * @param group 操作分组
     * @return 包含变更字段的新聚合实例；若为新聚合或未发生变更则返回 {@code null}
     * @apiNote 推荐使用 {@link #ifChanged(String)} 获取 Optional 返回值，避免空指针风险
     */
    public R changed(String group) {
        if (root.isNew()) {
            return null;
        }
        R result = newInstance(targetClass);
        if (scanChangedFields(this.targetClass, root, snapshot, group, (field, value) -> field.set(result, value))) {
            result.setVersion(root.getVersion());
            result.setId(root.getId());
            return result;
        }
        return null;
    }

    /**
     * 获取变更的字段名称集合
     */
    public Set<String> changedFields() {
        return changedFields(null);
    }

    /**
     * 获取分组情况下变更的字段名称集合
     *
     * @param group 分组名称
     */
    public Set<String> changedFields(String group) {
        Set<String> results = Sets.newHashSet();
        if (!root.isNew()) {
            scanChangedFields(this.targetClass, root, snapshot, group, (field, value) -> results.add(field.getName()));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public <I extends Serializable, T extends Entity<I>> Optional<T> changedEntity(Function<R, T> loader) {
        T        newEntity   = loader.apply(root);
        T        oldEntity   = loader.apply(snapshot);
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        T        result      = newInstance(entityClass);
        if (scanChangedFields(entityClass, newEntity, oldEntity, null, (field, value) -> field.set(result, value))) {
            result.setId(newEntity.getId());
            result.setVersion(newEntity.getVersion());
            return Optional.of(result);
        }
        return Optional.empty();
    }


    @FunctionalInterface
    private interface FieldCollector {
        void accept(Field field, Object value) throws Exception;
    }

    /**
     * 遍历字段对比新旧值，将变更字段交由 collector 收集。
     *
     * @param targetClass 待扫描的类
     * @param newSource   新值来源（root 或新实体）
     * @param oldSource   旧值来源（snapshot 或旧实体）
     * @param group       分组过滤，{@code null} 表示不按分组过滤
     * @param collector   变更收集器 ({@link Field}, 新值) → void
     * @return true 至少一个字段发生变更
     */
    private boolean scanChangedFields(Class<?> targetClass, Object newSource, Object oldSource, String group, FieldCollector collector) {
        try {
            Collection<Field> fields  = ReflectionUtils.getDeepDeclaredFields(targetClass);
            boolean           changed = false;
            for (Field field : fields) {
                if (shouldIgnore(field, group)) {
                    continue;
                }
                Object newValue = field.get(newSource);
                if (!DeepEquals.deepEquals(newValue, field.get(oldSource))) {
                    changed = true;
                    collector.accept(field, newValue);
                }
            }
            return changed;
        } catch (Exception e) {
            throw new AggregateException(e);
        }
    }

    /**
     * 判断字段是否应跳过变更扫描。
     */
    private boolean shouldIgnore(Field field, String group) {
        Ignore annotation = field.getAnnotation(Ignore.class);
        if (annotation == null) {
            return false;
        }
        return StringUtils.isBlank(group) || !Arrays.asList(annotation.group()).contains(group);
    }

    /**
     * 如果聚合中包含领域实体集合 获取新添加的领域实例集合
     */
    public <I extends Serializable, T extends Entity<I>> Collection<T> getNewEntities(Function<R, Collection<T>> getCollection) {
        if (this.root.isNew()) {
            return getCollection.apply(root);
        }
        return newEntities(getCollection);
    }

    /**
     * 统一按需收集实体集合的新增/删除/更新
     */
    public <I extends Serializable, T extends Entity<I>> ChangedResult<I, T> collect(Function<R, Collection<T>> getCollection) {
        return new ChangedResult<>(this, getCollection);
    }

    /**
     * 一次性收集实体集合的新增/删除/更新，返回包含全部三种变更的结果。
     */
    public <I extends Serializable, T extends Entity<I>> ChangedResult<I, T> all(Function<R, Collection<T>> getCollection) {
        return this.collect(getCollection).changed().added().removed();
    }

    /**
     * 如果聚合中含领域实体集合 获取该集合中变更的实体集合
     */
    public <I extends Serializable, T extends Entity<I>> List<T> changedEntities(Function<R, Collection<T>> getCollection) {
        return this.collect(getCollection).changed().getChanged();
    }

    /**
     * 如果聚合中包含领域实体集合 获取该集合中已删除的实体集合
     */
    public <I extends Serializable, T extends Entity<I>> List<T> removedEntities(Function<R, Collection<T>> getCollection) {
        return this.collect(getCollection).removed().getRemoved();
    }

    /**
     * 如果聚合中存在领域实体集合 获取新增的实体集合
     */
    public <I extends Serializable, T extends Entity<I>> List<T> newEntities(Function<R, Collection<T>> getCollection) {
        return this.collect(getCollection).added().getAdded();
    }

    /**
     * 比较两个集合的差异
     */
    public <V> CompareResult<V> compare(Function<R, Collection<V>> collector) {
        Set<V> newSet = Sets.newHashSet(Objects.requireNonNullElse(collector.apply(this.root), Collections.emptyList()));
        Set<V> oldSet = Sets.newHashSet(Objects.requireNonNullElse(collector.apply(this.snapshot), Collections.emptyList()));
        if (newSet.isEmpty() && oldSet.isEmpty()) {
            return new CompareResult<>(Collections.emptySet(), Collections.emptySet());
        }
        return new CompareResult<>(Sets.difference(newSet, oldSet), Sets.difference(oldSet, newSet));
    }

    /**
     * 获取实体id集合
     */
    private static <I extends Serializable, T extends Entity<I>> Set<I> entityIds(Collection<T> collection) {
        return collection.stream().map(Entity::getId).collect(Collectors.toSet());
    }

    /**
     * 获取指定标识的实体
     */
    public <I extends Serializable, T extends Entity<I>> T entity(Collection<T> entities, I id) {
        Preconditions.checkNotNull(id, "id must not be null.");
        return entities.stream().filter(e -> id.equals(e.getId())).findFirst().orElseThrow(() -> {
            String error = String.format("can not find entity by id: %s", id);
            return new AggregateException(error);
        });
    }

    private <T> T newInstance(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new AggregateException(e);
        }
    }

    @Getter
    public static class ChangedResult<I extends Serializable, T extends Entity<I>> {
        private final Collection<T> newEntities;
        private final Collection<T> oldEntities;
        private final Set<I>        newIds = Sets.newHashSet();
        private final Set<I>        oldIds = Sets.newHashSet();

        //变更收集结果
        private List<T> changed = Collections.emptyList();
        private List<T> added   = Collections.emptyList();
        private List<T> removed = Collections.emptyList();

        //判断是否计算
        private boolean addComputed    = false;
        private boolean changeComputed = false;
        private boolean removeComputed = false;

        public <K extends Serializable, R extends AggregateRoot<K>> ChangedResult(Aggregate<K, R> aggregate, Function<R, Collection<T>> getter) {
            this.newEntities = Objects.requireNonNullElse(getter.apply(aggregate.root), Collections.emptyList());
            this.oldEntities = Objects.requireNonNullElse(getter.apply(aggregate.snapshot), Collections.emptyList());
        }

        /**
         * 收集变化的子实体集合
         */
        public ChangedResult<I, T> changed() {
            if (changeComputed) {
                return this;
            }
            if (oldEntities.isEmpty() || newEntities.isEmpty()) {
                this.changeComputed = true;
                return this;
            }
            Map<I, T> oldEntityMap = oldEntities.stream().collect(Collectors.toMap(Entity::getId, v -> v));
            Map<I, T> newEntityMap = newEntities.stream().collect(Collectors.toMap(Entity::getId, v -> v));

            Set<I>  commonIds = Sets.intersection(oldEntityMap.keySet(), newEntityMap.keySet());
            List<T> results   = Lists.newArrayList();
            for (I id : commonIds) {
                T oldEntity = oldEntityMap.get(id);
                T newEntity = newEntityMap.get(id);
                if (!DeepEquals.deepEquals(oldEntity, newEntity)) {
                    results.add(newEntity);
                }
            }
            this.changed = results;
            this.changeComputed = true;
            return this;
        }

        /**
         * 收集新增的子实体集合
         */
        public ChangedResult<I, T> added() {
            if (addComputed) {
                return this;
            }
            lazyLoadEntityIds();
            Set<I> addIds = Sets.newHashSet(this.newIds);
            addIds.removeAll(oldIds);
            if (!addIds.isEmpty()) {
                this.added = newEntities.stream().filter(e -> addIds.contains(e.getId())).toList();
            }
            this.addComputed = true;
            return this;
        }

        /**
         * 收集删除子实体集合
         */
        public ChangedResult<I, T> removed() {
            if (removeComputed) {
                return this;
            }
            lazyLoadEntityIds();
            Set<I> oldTemp = Sets.newHashSet(this.oldIds);
            oldTemp.removeAll(newIds);
            if (!oldTemp.isEmpty()) {
                this.removed = oldEntities.stream().filter(e -> oldTemp.contains(e.getId())).toList();
            }
            this.removeComputed = true;
            return this;
        }

        /**
         * id集合懒加载计算
         */
        private void lazyLoadEntityIds() {
            if (this.newIds.isEmpty()) {
                this.newIds.addAll(entityIds(newEntities));
            }
            if (this.oldIds.isEmpty()) {
                this.oldIds.addAll(entityIds(oldEntities));
            }
        }
    }

}
