package com.boot.architect;

import com.cloud.arch.aggregate.AggregateRoot;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.EnumSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserInfo implements AggregateRoot<Long> {

    private static final long serialVersionUID = -8562409754697409465L;

    private Long          id;
    private String        name;
    private String        password;
    @JsonSerialize(using = EnumSerializer.class)
    private UserState     state;
    private Integer       version;
    private List<Integer> list;
    private LocalDateTime gmtCreate;

    public void modify() {
        this.name     = "周佳佳";
        this.password = "aqswdergtht";
        this.state    = UserState.NORMAL;
    }

    @Override
    public Integer getVersion() {
        return this.version;
    }

}
