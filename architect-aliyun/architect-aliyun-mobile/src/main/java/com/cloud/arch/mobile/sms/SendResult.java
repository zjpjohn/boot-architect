package com.cloud.arch.mobile.sms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {

    private Boolean success;
    private Integer code;
    private String  message;

    public static SendResult success(String message) {
        return new SendResult(true, 200, message);
    }

    public static SendResult apiError(String message) {
        return new SendResult(false, 500, message);
    }

    public static SendResult limitError(String message) {
        return new SendResult(false, 600, message);
    }

}
