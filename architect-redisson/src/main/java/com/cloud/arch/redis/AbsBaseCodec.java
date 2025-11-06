package com.cloud.arch.redis;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.codec.BaseCodec;

import java.io.IOException;

@Slf4j
public abstract class AbsBaseCodec extends BaseCodec {

    protected void warmupCodec() {
        ByteBuf byteBuf = null;
        try {
            byteBuf = this.getValueEncoder().encode("testValue");
            this.getValueDecoder().decode(byteBuf, null);
            byteBuf.release();
        } catch (IOException error) {
            log.error(error.getMessage(), error);
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }

}
