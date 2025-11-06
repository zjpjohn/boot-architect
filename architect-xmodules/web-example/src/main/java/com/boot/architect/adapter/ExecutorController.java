package com.boot.architect.adapter;

import com.boot.architect.application.command.ExecutorCommandService;
import com.boot.architect.infrast.persist.enums.Gender;
import com.boot.architect.infrast.persist.enums.State;
import com.cloud.arch.web.annotation.ApiBody;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/executor")
@RequiredArgsConstructor
public class ExecutorController {

    private final ExecutorCommandService commandService;

    @PostMapping("/gender")
    public void gender(@NotNull Gender gender) {
        commandService.execute(gender);
    }

    @PostMapping("/state")
    public void state(@NotNull State state) {
        commandService.execute(state);
    }

}
