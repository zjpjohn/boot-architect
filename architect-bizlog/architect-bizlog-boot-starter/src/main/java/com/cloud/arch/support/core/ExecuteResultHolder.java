package com.cloud.arch.support.core;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExecuteResultHolder {

    private boolean   success;
    private Throwable throwable;
    private String    message;

    public ExecuteResultHolder(boolean success) {
        this.success   = success;
        this.throwable = null;
        this.message   = "";
    }

    public ExecuteResultHolder(boolean success, Throwable throwable) {
        this.success   = success;
        this.throwable = throwable;
        this.message   = throwable.getMessage();
    }
}
