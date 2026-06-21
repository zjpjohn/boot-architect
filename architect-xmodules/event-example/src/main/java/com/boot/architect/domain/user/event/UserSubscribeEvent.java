package com.boot.architect.domain.user.event;

import com.cloud.arch.event.annotations.Subscribe;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Subscribe(name = "arch-user-topic", filter = "user-create", key = "userId")
public class UserSubscribeEvent {

    private Long   userId;
    private String name;
    private String phone;

}
