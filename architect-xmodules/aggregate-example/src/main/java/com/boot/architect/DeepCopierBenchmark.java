package com.boot.architect;

import com.cloud.arch.aggregate.ForyDeepCopier;
import com.google.common.collect.Lists;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Fork(1)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 2)
@State(Scope.Benchmark)
@Threads(10)
@Measurement(iterations = 10, time = 5)
public class DeepCopierBenchmark {

    private static final JsonDeepCopier jsonCopier = new JsonDeepCopier();
    private static final KryoDeepCopier kyroCopier = new KryoDeepCopier();
    private static final ForyDeepCopier furyCopier = ForyDeepCopier.instance();
    private static final UserInfo       user       = new UserInfo();

    static {
        user.setId(124556L);
        user.setName("周佳琪");
        user.setState(UserState.NORMAL);
        user.setPassword("1ddeqcetvtaodmoamdomdpqmpqmpqmadoqmpma09#9)pqmpdmqqoeqpp1e123x#p@q@w$d%m%qdqwoc");
        user.setVersion(1);
        user.setGmtCreate(LocalDateTime.now());
        List<Integer> list = Lists.newArrayList();
        for (int i = 0; i < 1024; i++) {
            list.add(i);
        }
        user.setList(list);
    }

    @Benchmark
    public void jsonTest() {
        jsonCopier.copy(user);
    }

    @Benchmark
    public void kyroTest() {
        kyroCopier.copy(user);
    }

    @Benchmark
    public void furyTest() {
        furyCopier.copy(user);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(DeepCopierBenchmark.class.getSimpleName()) // 要导入的测试类
                                          .build();
        new Runner(opt).run();
    }
}
