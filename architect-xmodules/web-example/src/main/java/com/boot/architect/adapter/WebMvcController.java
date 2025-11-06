package com.boot.architect.adapter;

import com.cloud.arch.web.annotation.ApiBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/web")
public class WebMvcController {

    @GetMapping("/test1")
    public String test1() {
        return "web mvc test";
    }

    @ApiBody(encrypt = true)
    @GetMapping("/encrypt")
    public String encrypt() {
        return "encrypt data";
    }

}
