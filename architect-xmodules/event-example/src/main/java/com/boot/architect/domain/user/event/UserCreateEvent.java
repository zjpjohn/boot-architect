package com.boot.architect.domain.user.event;

import com.cloud.arch.event.annotations.Publish;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Publish
//@Publish(name = "user-topic", filter = "user-create")
public class UserCreateEvent {

    private Long   userId;
    private String name;
    private String phone;

}
