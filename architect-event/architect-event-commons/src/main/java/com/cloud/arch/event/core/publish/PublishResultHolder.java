package com.cloud.arch.event.core.publish;

import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PublishResultHolder {

    public static final String EMPTY_MESSAGE = "";

    private       boolean         success;
    private       long            taken      = 0;
    private final List<Throwable> throwables = Lists.newLinkedList();

    public PublishResultHolder() {
        this.success = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getTaken() {
        return taken;
    }

    public void setTaken(long taken) {
        this.taken = taken;
    }

    public List<Throwable> getThrowables() {
        return Collections.unmodifiableList(throwables);
    }

    public void setThrowable(Throwable throwable) {
        this.throwables.add(throwable);
    }

    public String getErrorMsg() {
        if (CollectionUtils.isEmpty(throwables)) {
            return EMPTY_MESSAGE;
        }
        List<Throwable> errors = Lists.newArrayList(this.throwables);
        Collections.reverse(errors);
        return errors.stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining(";"));
    }

}
