package com.cloud.arch.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;

@Slf4j
public class FastjsonCodec extends AbsBaseCodec {

    private final Encoder         encoder;
    private final Decoder<Object> decoder;

    public FastjsonCodec() {
        super();
        this.encoder = new JsonEncoder();
        this.decoder = new JsonDecoder();
        this.warmupCodec();
    }

    @Override
    public Decoder<Object> getValueDecoder() {
        return this.decoder;
    }

    @Override
    public Encoder getValueEncoder() {
        return this.encoder;
    }

    public static class JsonEncoder implements Encoder {
        @Override
        public ByteBuf encode(Object value) throws IOException {
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream outputStream = new ByteBufOutputStream(buffer);
                JSON.writeTo(outputStream, value, JSONWriter.Feature.WriteClassName);
                return outputStream.buffer();
            } catch (Exception error) {
                buffer.release();
                throw new IOException(error);
            }
        }
    }

    public static class JsonDecoder implements Decoder<Object> {

        private final Filter autoTypeFiler;

        public JsonDecoder() {
            this.autoTypeFiler = JSONReader.autoTypeFilter(CodecTypeUtils.typList());
        }

        @Override
        public Object decode(ByteBuf buf, State state) throws IOException {
            try (ByteBufInputStream stream = new ByteBufInputStream(buf)) {
                byte[] byteArray = new byte[stream.available()];
                int    result    = stream.read(byteArray, 0, byteArray.length);
                if (result <= 0) {
                    return null;
                }
                return JSON.parseObject(byteArray, Object.class, autoTypeFiler);
            }
        }
    }
}
