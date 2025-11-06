package com.cloud.arch.event.remoting;


import com.cloud.arch.event.reparation.ReparationResponse;

public interface RemotingResponseHandler {

    /**
     * 响应成功处理
     *
     * @param response server端响应内容
     */
    void onHandle(ReparationResponse response);

    default void onError(Long eventId, Throwable throwable) {
    }
}
