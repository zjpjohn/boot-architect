package com.boot.architect.adapter;

import com.cloud.arch.web.annotation.ApiBody;
import com.cloud.arch.web.annotation.ApiVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/version")
public class WebVersionController {

    @GetMapping("/test1")
    public String test1() {
        return "version test";
    }

    @ApiVersion("1.1")
    @GetMapping("/test1")
    public String testV1() {
        return "version test version[1.1]";
    }

    @ApiVersion("1.2")
    @GetMapping("/test1")
    public String testV2() {
        return "version test version[1.2]";
    }

}
