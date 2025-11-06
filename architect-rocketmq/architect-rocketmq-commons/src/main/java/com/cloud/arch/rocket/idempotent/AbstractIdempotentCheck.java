package com.cloud.arch.rocket.idempotent;

public abstract class AbstractIdempotentCheck implements IdempotentChecker {

    private Class<Exception> retryFor;
    private Class<Exception> idempotentFor;
    private boolean          ignoreOnFail;

    /**
     * 判断消息是否已经处理
     *
     * @param key 消息标识
     * @param cls 幂等校验类型
     */
    @Override
    public boolean isProcessed(String key, Integer cls) {
        try {
            return doProcessed(key, cls);
        } catch (Exception e) {
            if (isIgnoreOnFail()) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    public abstract boolean doProcessed(String key, Integer cls) throws Exception;

    /**
     * 标记消息是否已经处理
     *
     * @param key 校验标识
     * @param cls 幂等校验类型
     * @param e   异常
     */
    @Override
    public void markProcessed(String key, Integer cls, Throwable e) {
        if (e instanceof Exception) {
            if (idempotentFor != null && idempotentFor.isAssignableFrom(e.getClass())) {
                markSuccess(key, cls);
                return;
            }
            if (retryFor != null && retryFor.isAssignableFrom(e.getClass())) {
                markFailed(key, cls);
                return;
            }
            markFailed(key, cls);
            return;
        }
        markSuccess(key, cls);
    }

    /**
     * 标记消息处理完成
     *
     * @param key 消息标识
     */
    public abstract void markSuccess(String key, Integer cls);

    /**
     * 标记消息处理失败
     *
     * @param key 消息标识
     */
    public abstract void markFailed(String key, Integer cls);

    public Class<Exception> getRetryFor() {
        return retryFor;
    }

    public void setRetryFor(Class<Exception> retryFor) {
        this.retryFor = retryFor;
    }

    public Class<Exception> getIdempotentFor() {
        return idempotentFor;
    }

    public void setIdempotentFor(Class<Exception> idempotentFor) {
        this.idempotentFor = idempotentFor;
    }

    public boolean isIgnoreOnFail() {
        return ignoreOnFail;
    }

    public void setIgnoreOnFail(boolean ignoreOnFail) {
        this.ignoreOnFail = ignoreOnFail;
    }
}
