package com.cloud.arch.event.publisher;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.utils.KafkaEventConstants;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> template;

    public KafkaEventPublisher(KafkaTemplate<String, String> template) {
        this.template = template;
    }

    /**
     * 发布跨应用领域事件
     *
     * @param message 事件消息
     */
    @Override
    public void publish(EventMessage message) {
        ProducerRecord<String, String> producerRecord = this.checkAndConvert(message);
        template.send(producerRecord);
    }

    /**
     * 事件校验转换
     *
     * @param message 领域事件消息
     */
    private ProducerRecord<String, String> checkAndConvert(EventMessage message) {
        Assert.state(StringUtils.hasText(message.getName()), "消息topic不允许为空.");
        Assert.state(StringUtils.hasText(message.getData()), "消息内容不允许为空.");
        Assert.state(StringUtils.hasText(message.getKey()), "消息业务key不允许为空.");
        RecordHeaders headers = new RecordHeaders();
        headers.add(KafkaEventConstants.EVENT_KEY, message.getKey().getBytes(StandardCharsets.UTF_8));
        return new ProducerRecord<>(message.getName(),
                                    null,
                                    System.currentTimeMillis(),
                                    message.getKey(),
                                    message.getData(),
                                    headers);
    }

}
