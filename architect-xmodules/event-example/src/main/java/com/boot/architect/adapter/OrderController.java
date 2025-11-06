package com.boot.architect.adapter;

import com.boot.architect.application.command.IOrderCommandService;
import com.boot.architect.application.query.IOrderQueryService;
import com.cloud.arch.web.annotation.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderCommandService orderCommandService;
    private final IOrderQueryService   orderQueryService;

}
