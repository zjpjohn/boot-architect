package com.boot.architect.domain.user.model;

import com.boot.architect.domain.user.event.UserCreateEvent;
import com.cloud.arch.aggregate.AggregateRoot;
import com.cloud.arch.event.publisher.DomainEventPublisher;
import com.cloud.arch.utils.IdWorker;
import lombok.Data;

@Data
public class UserInfoDo implements AggregateRoot<Long> {

    private Long    id;
    private String  name;
    private String  phone;
    private Integer state;

    public UserInfoDo(String name, String phone) {
        this.id    = IdWorker.nextId();
        this.name  = name;
        this.phone = phone;
    }

    public void create() {
        this.state = 1;
        UserCreateEvent event = new UserCreateEvent(this.id, this.name, this.phone);
        DomainEventPublisher.publish(event);
    }

}
