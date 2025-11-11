package com.boot.architect.adapter;

import com.cloud.arch.web.annotation.ApiBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        return "web mvc test1";
    }

    @ApiBody(encrypt = true)
    @GetMapping("/encrypt")
    public String encrypt() {
        return "encrypted data";
    }

    @GetMapping("/test2/{data}")
    public String test2(@PathVariable String data) {
        return "web mvc test2 " + data;
    }

}
