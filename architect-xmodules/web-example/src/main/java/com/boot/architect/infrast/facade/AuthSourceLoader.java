package com.boot.architect.infrast.facade;

import com.cloud.arch.web.IHttpAuthSourceManager;
import com.cloud.arch.web.impl.DefaultHttpAuthSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSourceLoader implements CommandLineRunner {

    private final IHttpAuthSourceManager sourceManager;

    @Override
    public void run(String... args) throws Exception {
        sourceManager.addSource(new DefaultHttpAuthSource("userId", "user"));
        sourceManager.addSource(new DefaultHttpAuthSource("userId", "manager"));
    }

}
