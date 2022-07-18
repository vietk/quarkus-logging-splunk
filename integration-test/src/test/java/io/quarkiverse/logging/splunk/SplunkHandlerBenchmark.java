package io.quarkiverse.logging.splunk;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class SplunkHandlerBenchmark {

    ExtHandler handler;

    ExtLogRecord record = new ExtLogRecord(Level.ALL, "log message", SplunkHandlerBenchmark.class.getName());

    SplunkResource resource;

    @Setup
    public void setup() {
        resource = new SplunkResource();
        Map<String, String> splunkInfo = resource.start();

        SplunkLogHandlerRecorder recorder = new SplunkLogHandlerRecorder();
        SplunkConfig config = new SplunkConfig();
        config.enabled = true;
        config.token = Optional.of("29fe2838-cab6-4d17-a392-37b7b8f41f75");
        config.channel = Optional.empty();
        config.batchInterval = Duration.ofSeconds(10);
        config.batchSizeBytes = 10;
        config.batchSizeCount = 10;
        config.sendMode = SplunkConfig.SendMode.SEQUENTIAL;
        config.metadataIndex = Optional.empty();
        config.metadataHost = Optional.empty();
        config.metadataSource = Optional.empty();
        config.format = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n";
        config.level = Level.ALL;
        config.disableCertificateValidation = true;
        config.url = splunkInfo.get("quarkus.log.handler.splunk.url");
        AsyncConfig asyncConfig = new AsyncConfig();
        asyncConfig.enable = true;
        asyncConfig.queueLength = 512;
        asyncConfig.overflow = AsyncHandler.OverflowAction.BLOCK;
        config.async = asyncConfig;
        var optionalRuntimeValue = recorder.initializeHandler(config);
        handler = (ExtHandler) optionalRuntimeValue.getValue().get();
    }

    @TearDown
    public void tearDown() {
        resource.stop();
        handler.close();
    }

    @Benchmark
    @Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
    @Fork(1)
    @Threads(10)
    public void benchSequentialLogs() {
        handler.publish(record);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .forks(1)
                .addProfiler(JavaFlightRecorderProfiler.class, "configName=profile")
                //        .jvmArgs("-Xmx512m", "-Xms512m"
                //            "-Djmh.stack.profiles=" + destinationFolder,
                //            "-Djmh.executor=FJP",
                //            "-Djmh.fr.options=defaultrecording=true,settings=" + profile)
                .include(SplunkHandlerBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
