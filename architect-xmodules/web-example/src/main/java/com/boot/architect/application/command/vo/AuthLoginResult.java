package com.boot.architect.application.command.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AuthLoginResult {

    private String token;
    private Date   expireAt;

}
