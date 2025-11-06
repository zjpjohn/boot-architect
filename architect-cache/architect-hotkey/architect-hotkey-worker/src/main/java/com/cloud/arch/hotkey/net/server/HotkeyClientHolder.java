package com.cloud.arch.hotkey.net.server;

import com.cloud.arch.hotkey.net.model.AppInfo;
import com.cloud.arch.hotkey.utils.NettyIpUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class HotkeyClientHolder {

    private static final String ATTR_APP_NAME = "client_app_name";

    private final Map<String, AppInfo> appCache = Maps.newConcurrentMap();

    public HotkeyClientHolder() {
    }

    public synchronized void newClient(String appName, String ip, ChannelHandlerContext context) {
        log.info("{} new client ip[{}] join.", appName, ip);
        //连接成功设置app_name属性值
        AttributeKey<String> attributeKey = AttributeKey.valueOf(ATTR_APP_NAME);
        context.channel().attr(attributeKey).set(appName);
        AppInfo appInfo = Optional.ofNullable(appCache.get(appName))
                .orElseGet(() -> new AppInfo(appName));
        appInfo.add(context);
        appCache.put(appName, appInfo);
    }

    public synchronized void loseClient(ChannelHandlerContext context) {
        AttributeKey<String> attributeKey = AttributeKey.valueOf(ATTR_APP_NAME);
        final String         appName      = context.channel().attr(attributeKey).get();
        if (StringUtils.isNotBlank(appName)) {
            Optional.ofNullable(appCache.get(appName))
                    .ifPresent(app -> app.remove(context));
        }
        log.info("client removed ip[{}]", NettyIpUtil.clientIp(context));
    }

    public List<AppInfo> getAppList() {
        return Lists.newArrayList(appCache.values());
    }

}
