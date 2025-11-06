package com.cloud.arch.hotkey.network.worker;


import io.netty.channel.Channel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class HotKeyWorker implements Comparable<HotKeyWorker> {

    private String  address;
    private Channel channel;

    public HotKeyWorker(String address, Channel channel) {
        this.address = address;
        this.channel = channel;
    }

    @Override
    public int compareTo(HotKeyWorker target) {
        return this.address.compareTo(target.getAddress());
    }

    public boolean channelActive() {
        return channel != null && channel.isActive();
    }

}
