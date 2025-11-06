package com.boot.architect.adapter;

import com.cloud.arch.web.annotation.ApiBody;
import com.cloud.arch.web.annotation.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/security")
public class WebSecurityController {

    @Permission(domain = "user", permit = "read")
    @GetMapping("/sec1")
    public String security1() {
        return "权限接口1";
    }

    @Permission(domain = "user", permit = "read", role = "role1")
    @GetMapping("/sec2")
    public String security2() {
        return "权限接口2";
    }

    @Permission(domain = "manager", permit = "write")
    @PostMapping("/sec1")
    public String security3() {
        return "权限接口3";
    }

    @Permission(domain = "manager", permit = "edit", role = "role1")
    @PutMapping("/sec1")
    public String security4() {
        return "权限接口4";
    }

    @PostMapping("/sec5")
    public String security5() {
        return "权限接口5";
    }

    @PostMapping("/sec6")
    public String security6() {
        return "权限接口6";
    }

    @Permission(domain = "manager")
    @GetMapping("/sec7")
    public String security7() {
        return "权限接口7";
    }

    @GetMapping("/sec8")
    public String security8() {
        return "无权限校验接口8";
    }

}
