package com.cloud.arch.event.props;

import lombok.Data;


@Data
public class RabbitmqProperties {

    /**
     * 生产者交换机名称
     */
    private Exchange producer = new Exchange();
    /**
     * 消费者交换机
     */
    private Exchange consumer = new Exchange();

    @Data
    public static class Exchange {

        private String exchange = "";

    }

}
