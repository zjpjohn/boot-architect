package com.cloud.arch.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring 集成测试基类，加载完整 ApplicationContext。
 * 需要调用方提供 {@code @SpringBootApplication} 主类或通过
 * {@code @ContextConfiguration} 指定配置。
 *
 * <pre>{@code
 * @SpringBootTest(classes = MyTestConfig.class)
 * class MyServiceIT extends BaseSpringTest {
 *     @Autowired MyService service;
 *
 *     @Test
 *     void shouldWorkEndToEnd() { ... }
 * }
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseSpringTest {
}
