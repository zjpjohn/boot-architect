package com.cloud.arch.event;

import com.cloud.arch.event.remoting.HttpRemoting;
import com.cloud.arch.event.reparation.ReparationConstants;
import com.cloud.arch.event.reparation.ReparationRequest;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.cloud.arch.event.utils.Threads;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class RocksReparationProcessor {

    private final HttpRemoting              httpRemoting;
    private final RocksCompensateProperties properties;
    private final List<String>              servers;
    private final ExecutorService           pushExecutor;

    public RocksReparationProcessor(HttpRemoting httpRemoting, RocksCompensateProperties properties) {
        this.httpRemoting = httpRemoting;
        this.properties   = properties;
        this.servers      = this.parseAndSplit();
        this.pushExecutor = new ThreadPoolExecutor(properties.getPushThreads(),
                                                   properties.getPushMaxThreads(),
                                                   1,
                                                   TimeUnit.MINUTES,
                                                   new LinkedBlockingQueue<>(properties.getPushQueueSize()),
                                                   Threads.threadFactory("reparation-http-request"));
    }

    /**
     * 请求上传补偿发送事件
     *
     * @param entity 事件内容
     */
    public void push(PublishEventEntity entity) {
        this.pushExecutor.submit(() -> {
            final ReparationRequest request    = this.convert(entity);
            final String            requestUri = this.selectServer() + ReparationConstants.REPARATION_PATH;
            this.httpRemoting.post(requestUri, request);
        });
    }

    /**
     * 解析校验补偿服务器集合
     */
    private List<String> parseAndSplit() {
        final String servers = this.properties.getServers();
        Assert.state(StringUtils.hasText(servers), "补偿服务server列表为空.");
        return Arrays.asList(servers.split(","));
    }

    /**
     * 随机选择请求服务器
     */
    private String selectServer() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        final int         index  = random.nextInt(this.servers.size());
        return this.servers.get(index);
    }

    /**
     * 请求转换
     *
     * @param entity 事件内容
     */
    private ReparationRequest convert(PublishEventEntity entity) {
        final ReparationRequest request = new ReparationRequest();
        request.setEventId(entity.getId());
        request.setKeys(entity.getId().toString());
        request.setTopic(entity.getName());
        request.setTag(entity.getFilter());
        request.setDelay(entity.getDelay());
        request.setBody(entity.getEvent());
        request.setBizGroup(entity.getBizGroup());
        request.setEventTime(entity.getGmtCreate());
        return request;
    }

}
