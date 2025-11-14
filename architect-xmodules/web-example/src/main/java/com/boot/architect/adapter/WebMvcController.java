package com.boot.architect.adapter;

import com.boot.architect.application.query.dto.Gender;
import com.cloud.arch.web.annotation.ApiBody;
import com.cloud.arch.web.domain.ApiReturn;
import com.cloud.arch.web.domain.BodyData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

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

    @GetMapping("/test3")
    public ApiReturn<String> test3() {
        return ApiReturn.success("web mvc test3");
    }

    @GetMapping("/test4")
    public BodyData<String> test4() {
        return new BodyData<>("web mvc test4", 200, null);
    }

    @GetMapping("/test5")
    public void test5() {
    }

    @GetMapping("/test6")
    public Gender test6() {
        return Gender.FEMALE;
    }

    @GetMapping(value = "/sse")
    public ResponseBodyEmitter emitter() {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    emitter.send("SSE - " + i + "\n");
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @GetMapping(value = "/sse1")
    public SseEmitter sseEmitter() {
        SseEmitter emitter = new SseEmitter();
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    emitter.send("SSE - " + i + "\n");
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

}
