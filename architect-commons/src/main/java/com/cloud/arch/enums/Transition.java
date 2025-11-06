package com.cloud.arch.enums;

import java.util.Collections;
import java.util.Set;

public interface Transition<K extends Comparable<K>, T extends Transition<K, T>> extends Value<K> {

    default Set<T> transitions() {
        return Collections.emptySet();
    }

}
