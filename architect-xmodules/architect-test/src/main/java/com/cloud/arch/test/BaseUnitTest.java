package com.cloud.arch.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 纯单元测试基类，集成 Mockito 扩展。
 * 适用于不依赖 Spring 容器、不需要外部中间件的纯逻辑测试。
 *
 * <pre>{@code
 * class MyServiceTest extends BaseUnitTest {
 *     @Mock MyRepository repository;
 *     @InjectMocks MyService service;
 *
 *     @Test
 *     void shouldReturnData_whenRepositoryReturns() { ... }
 * }
 * }</pre>
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
}
