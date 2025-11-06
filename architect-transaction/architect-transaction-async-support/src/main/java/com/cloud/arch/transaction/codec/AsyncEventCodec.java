package com.cloud.arch.transaction.codec;


import com.cloud.arch.transaction.core.AsyncTxParams;

public interface AsyncEventCodec {

    String encode(AsyncTxParams params);

    AsyncTxParams decode(String data);

}
