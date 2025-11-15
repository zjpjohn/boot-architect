package com.cloud.arch.aggregate;

import com.cloud.arch.aggregate.reflection.DeepEquals;
import com.cloud.arch.aggregate.reflection.ReflectionUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;

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
        this.root        = root;
        this.targetClass = (Class<R>) root.getClass();
        this.snapshot    = copier.copy(root);
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
    public Optional<R> ifChanged() {
        return Optional.ofNullable(this.changed());
    }

    /**
     * 获取更新数据后的聚合实例
     */
    public R changed() {
        if (!this.isNew()) {
            try {
                R                 result  = newInstance(this.targetClass);
                Collection<Field> fields  = ReflectionUtils.getDeepDeclaredFields(this.targetClass);
                boolean           changed = false;
                for (Field field : fields) {
                    if (field.getAnnotation(Ignore.class) != null) {
                        continue;
                    }
                    Object value = field.get(root);
                    if (!DeepEquals.deepEquals(value, field.get(snapshot))) {
                        field.set(result, value);
                        changed = true;
                    }
                }
                if (changed) {
                    result.setVersion(root.getVersion());
                    result.setId(root.getId());
                    return result;
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
        return null;
    }

    /**
     * 获取分组情况下更新数据后的聚合根实例
     *
     * @param group 分组信息
     */
    public Optional<R> ifChanged(String group) {
        return Optional.ofNullable(this.changed(group));
    }

    /**
     * 获取分组情况下更新数据后的聚合实例
     *
     * @param group 操作分组
     */
    public R changed(String group) {
        if (!this.root.isNew()) {
            try {
                R                 result  = newInstance(targetClass);
                Collection<Field> fields  = ReflectionUtils.getDeepDeclaredFields(this.targetClass);
                boolean           changed = false;
                for (Field field : fields) {
                    Ignore annotation = field.getAnnotation(Ignore.class);
                    if (annotation != null && !Arrays.asList(annotation.group()).contains(group)) {
                        continue;
                    }
                    Object value = field.get(root);
                    if (!DeepEquals.deepEquals(value, field.get(snapshot))) {
                        changed = true;
                        field.set(result, value);
                    }
                }
                if (changed) {
                    result.setVersion(root.getVersion());
                    result.setId(root.getId());
                    return result;
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
        return null;
    }

    /**
     * 获取变更的字段名称集合
     */
    public Set<String> changedFields() {
        Set<String> results = Sets.newHashSet();
        if (!this.root.isNew()) {
            try {
                Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(this.targetClass);
                for (Field field : fields) {
                    if (field.getAnnotation(Ignore.class) != null) {
                        continue;
                    }
                    Object value = field.get(root);
                    if (!DeepEquals.deepEquals(value, field.get(snapshot))) {
                        results.add(field.getName());
                    }
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
        return results;
    }

    /**
     * 获取分组情况下变更的字段名称集合
     *
     * @param group 分组名称
     */
    public Set<String> changedFields(String group) {
        Set<String> results = Sets.newHashSet();
        if (!this.root.isNew()) {
            try {
                Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(this.targetClass);
                for (Field field : fields) {
                    Ignore annotation = field.getAnnotation(Ignore.class);
                    if (annotation != null && !Lists.newArrayList(annotation.group()).contains(group)) {
                        continue;
                    }
                    if (!DeepEquals.deepEquals(field.get(root), field.get(snapshot))) {
                        results.add(field.getName());
                    }
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public <I extends Serializable, T extends AggregateRoot<I>> Optional<T> changedEntity(Function<R, T> getEntity) {
        T        newEntity   = getEntity.apply(root);
        T        oldEntity   = getEntity.apply(snapshot);
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        try {
            T                 result  = newInstance(entityClass);
            Collection<Field> fields  = ReflectionUtils.getDeepDeclaredFields(entityClass);
            boolean           changed = false;
            for (Field field : fields) {
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                Object value = field.get(newEntity);
                if (!DeepEquals.deepEquals(value, field.get(oldEntity))) {
                    changed = true;
                    field.set(result, value);
                }
            }
            if (changed) {
                result.setVersion(newEntity.getVersion());
                result.setId(newEntity.getId());
                return Optional.of(result);
            }
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        return Optional.empty();
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
     * 如果聚合中含领域实体集合 获取该集合中变更的实体集合
     */
    public <I extends Serializable, T extends Entity<I>> List<T> changedEntities(Function<R, Collection<T>> getCollection) {
        Collection<T> oldEntities = getCollection.apply(snapshot);
        Collection<T> newEntities = getCollection.apply(root);

        Map<I, T> oldEntityMap = oldEntities.stream().collect(Collectors.toMap(Entity::getId, v -> v));
        Map<I, T> newEntityMap = newEntities.stream().collect(Collectors.toMap(Entity::getId, v -> v));

        // 取新老数据的集合的交集
        oldEntityMap.keySet().retainAll(newEntityMap.keySet());
        List<T> results = Lists.newArrayList();
        for (Map.Entry<I, T> entry : oldEntityMap.entrySet()) {
            T oldEntity = entry.getValue();
            T newEntity = newEntityMap.get(entry.getKey());
            if (!DeepEquals.deepEquals(oldEntity, newEntity)) {
                results.add(newEntity);
            }
        }
        return results;
    }

    /**
     * 如果聚合中包含领域实体集合 获取该集合中已删除的实体集合
     */
    public <I extends Serializable, T extends Entity<I>> List<T> removedEntities(Function<R, Collection<T>> getCollection) {
        Collection<T> oldEntities = getCollection.apply(snapshot);
        Set<I>        newIds      = entityIds(getCollection.apply(root));
        Set<I>        oldIds      = entityIds(oldEntities);
        oldIds.removeAll(newIds);
        return oldEntities.stream().filter(e -> oldIds.contains(e.getId())).toList();
    }

    /**
     * 如果聚合中存在领域实体集合 获取新增的实体集合
     */
    public <I extends Serializable, T extends Entity<I>> List<T> newEntities(Function<R, Collection<T>> getCollection) {
        Collection<T> newEntities = getCollection.apply(root);
        Set<I>        newIds      = entityIds(newEntities);
        Set<I>        oldIds      = entityIds(getCollection.apply(snapshot));
        newIds.removeAll(oldIds);
        return newEntities.stream().filter(e -> newIds.contains(e.getId())).toList();
    }

    /**
     * 集合转换为Set
     */
    private static <V> Set<V> convertToSet(Collection<V> value) {
        if (value instanceof Set<V> vSet) {
            return vSet;
        }
        return Sets.newHashSet(value);
    }

    /**
     * 比较两个集合的差异
     */
    public static <V> CompareResult<V> compare(Collection<V> newValues, Collection<V> oldValues) {
        Set<V> newSet       = convertToSet(newValues);
        Set<V> oldSet       = convertToSet(oldValues);
        Set<V> intersection = Sets.intersection(newSet, oldSet);
        Set<V> deleted      = Sets.difference(oldSet, intersection);
        Set<V> added        = Sets.difference(newSet, intersection);
        return new CompareResult<>(added, deleted);
    }

    /**
     * 获取实体id集合
     */
    private <I extends Serializable, T extends Entity<I>> Set<I> entityIds(Collection<T> collection) {
        return collection.stream().map(Entity::getId).collect(Collectors.toSet());
    }

    /**
     * 获取指定标识的实体
     */
    public <I extends Serializable, T extends Entity<I>> T entity(Collection<T> entities, I id) {
        Preconditions.checkNotNull(id, "id must not be null.");
        return entities.stream().filter(e -> id.equals(e.getId())).findFirst().orElseThrow(() -> {
            String error = String.format("can not find entity by id: %s", id);
            return new NullPointerException(error);
        });
    }

    private <T> T newInstance(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
