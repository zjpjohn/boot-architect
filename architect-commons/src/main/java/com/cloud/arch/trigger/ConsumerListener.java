package com.cloud.arch.trigger;

import java.util.List;

public interface ConsumerListener<E> {

    void handle(List<E> events);

}
