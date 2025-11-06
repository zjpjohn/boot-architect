package com.cloud.arch.event.reparation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ReparationResponse implements Serializable {

    private static final long serialVersionUID = -3491410758322681528L;

    private static final Integer SUCCESS       = 200;
    private static final Integer ILLEGAL_EVENT = 400;
    private static final Integer UN_AUTHORIZED = 401;
    private static final Integer NOT_FOUND     = 401;
    private static final Integer METHOD_ERROR  = 405;
    private static final Integer SERVER_ERROR  = 500;

    private Long    eventId;
    private Integer code;
    private String  message;

    public ReparationResponse(Long eventId, Integer code, String message) {
        this.eventId = eventId;
        this.code    = code;
        this.message = message;
    }

    public ReparationResponse(Integer code, String message) {
        this.code    = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return SUCCESS.equals(this.code);
    }

    public static ReparationResponse success(Long eventId, String message) {
        return new ReparationResponse(eventId, SUCCESS, message);
    }

    public static ReparationResponse unAuthorized(String message) {
        return new ReparationResponse(UN_AUTHORIZED, message);
    }

    public static ReparationResponse error(Long eventId, String message) {
        return new ReparationResponse(eventId, SERVER_ERROR, message);
    }

    public static ReparationResponse methodError(String message) {
        return new ReparationResponse(METHOD_ERROR, message);
    }

    public static ReparationResponse netError(String message) {
        return new ReparationResponse(SERVER_ERROR, message);
    }

    public static ReparationResponse notFound(String message) {
        return new ReparationResponse(NOT_FOUND, message);
    }

    public static ReparationResponse badRequest(Long eventId, String message) {
        return new ReparationResponse(eventId, ILLEGAL_EVENT, message);
    }
}
