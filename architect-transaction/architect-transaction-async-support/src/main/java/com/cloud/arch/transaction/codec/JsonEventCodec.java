package com.cloud.arch.transaction.codec;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.transaction.core.AsyncTxParams;

public class JsonEventCodec implements AsyncEventCodec {

    @Override
    public String encode(AsyncTxParams context) {
        return JSON.toJSONString(context);
    }

    @Override
    public AsyncTxParams decode(String data) {
        return JSON.parseObject(data, AsyncTxParams.class);
    }

}
