package com.cloud.arch.hotkey.net.filter.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.SystemClock;
import com.cloud.arch.hotkey.config.props.HotKeyProperties;
import com.cloud.arch.hotkey.counter.KeyCounterProcessor;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.net.filter.ICommandFilter;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.List;

@Slf4j
public class KeyCountCommandFilter implements ICommandFilter {

    private final KeyCounterProcessor processor;
    private final HotKeyProperties    properties;

    @Inject
    public KeyCountCommandFilter(KeyCounterProcessor processor,
                                 HotKeyProperties properties) {
        this.processor  = processor;
        this.properties = properties;
    }

    @Override
    public void handle(HotkeyCommand command, ChannelHandlerContext context) {
        List<KeyCountModel> models = command.getKeyCountModels();
        if (CollectionUtil.isEmpty(models)) {
            return;
        }
        String appName = command.getAppName();
        long   timeout = SystemClock.now() - models.get(0).getCreateTime();
        //key统计信息超过5秒不处理
        if (StringUtils.isBlank(appName)
                && timeout > properties.getExpireThreshold() + 10000) {
            return;
        }
        processor.publish(appName, models);
    }

}
