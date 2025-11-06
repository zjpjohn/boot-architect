package com.cloud.arch.event.subscribe;

public abstract class AbstractIdempotentChecker implements IdempotentChecker {

    private Class<Exception> retryFor;
    private Class<Exception> idempotentFor;
    private boolean          ignoreOnFail;

    /**
     * 判断消息是否已经处理
     */
    @Override
    public boolean isProcessed(EventIdempotent idempotent) {
        try {
            return doProcessed(idempotent);
        } catch (Exception e) {
            if (isIgnoreOnFail()) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    public abstract boolean doProcessed(EventIdempotent idempotent) throws Exception;

    /**
     * 标记消息是否已经处理
     *
     * @param idempotent 幂等信息
     * @param e          异常
     */
    @Override
    public void markProcessed(EventIdempotent idempotent, Throwable e) {
        if (e instanceof Exception) {
            if (idempotentFor != null && idempotentFor.isAssignableFrom(e.getClass())) {
                markSuccess(idempotent);
                return;
            }
            if (retryFor != null && retryFor.isAssignableFrom(e.getClass())) {
                markFailed(idempotent);
                return;
            }
            markFailed(idempotent);
            return;
        }
        markSuccess(idempotent);
    }

    /**
     * 标记消息处理完成
     *
     * @param idempotent 幂等信息
     */
    public abstract void markSuccess(EventIdempotent idempotent);

    /**
     * 标记消息处理失败
     *
     * @param idempotent 幂等信息
     */
    public abstract void markFailed(EventIdempotent idempotent);

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
