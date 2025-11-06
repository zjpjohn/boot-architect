package com.cloud.arch.event.reparation;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ReparationRequest {

    private Long   eventId;
    private String bizGroup = "";
    private String keys;
    private String headers;
    private String topic;
    private String tag;
    private Long   delay;
    private String body;
    private Long   eventTime;

    public boolean validate() {
        return eventId != null
                && StringUtils.isNotBlank(topic)
                && StringUtils.isNotBlank(body);
    }
}
