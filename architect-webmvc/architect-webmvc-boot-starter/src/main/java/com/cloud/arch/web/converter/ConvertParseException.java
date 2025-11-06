package com.cloud.arch.web.converter;

import org.springframework.core.convert.ConversionException;

public class ConvertParseException extends ConversionException {

    public ConvertParseException(String message) {
        super(message);
    }

    public ConvertParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
