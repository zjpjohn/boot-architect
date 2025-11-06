package com.cloud.arch.hotkey.convert;

import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.WireFormat;
import io.protostuff.runtime.Delegate;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;

public class LongAdderDelegate implements Delegate<LongAdder> {
    @Override
    public WireFormat.FieldType getFieldType() {
        return WireFormat.FieldType.INT64;
    }

    @Override
    public LongAdder readFrom(Input input) throws IOException {
        LongAdder adder = new LongAdder();
        adder.add(input.readInt64());
        return adder;
    }

    @Override
    public void writeTo(Output output, int index, LongAdder adder, boolean repeated) throws IOException {
        output.writeInt64(index, adder.sum(), repeated);
    }

    @Override
    public void transfer(Pipe pipe, Input input, Output output, int index, boolean repeated) throws IOException {
        output.writeInt64(index, input.readInt64(), repeated);
    }

    @Override
    public Class<?> typeClass() {
        return LongAdder.class;
    }

}
