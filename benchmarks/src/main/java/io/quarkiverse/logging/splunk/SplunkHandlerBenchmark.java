package io.quarkiverse.logging.splunk;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.handlers.AsyncHandler;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import io.quarkus.runtime.logging.DiscoveredLogComponents;

@State(Scope.Benchmark)
public class SplunkHandlerBenchmark {

    ExtHandler handler;

    ExtLogRecord record = new ExtLogRecord(Level.ALL, "log message", SplunkHandlerBenchmark.class.getName());

    ClientAndServer httpServer;

    @Param("0")
    long mockDelayMs = 0;

    @Param("PARALLEL")
    String hecSendMode = "PARALLEL";

    @Param("true")
    boolean useAsyncHandler = true;

    @Setup
    public void setup() {
        SplunkLogHandlerRecorder recorder = new SplunkLogHandlerRecorder();
        SplunkConfig parentConfig = new SplunkConfig();
        SplunkHandlerConfig config = new SplunkHandlerConfig();
        parentConfig.config = config;
        config.enabled = true;
        config.token = Optional.of("29fe2838-cab6-4d17-a392-37b7b8f41f75");
        config.channel = Optional.empty();
        config.batchInterval = Duration.ofSeconds(10);
        config.batchSizeBytes = 10;
        config.batchSizeCount = 10;
        config.sendMode = SplunkHandlerConfig.SendMode.valueOf(hecSendMode.toUpperCase());
        config.metadataIndex = Optional.empty();
        config.metadataHost = Optional.empty();
        config.metadataSource = Optional.empty();
        config.metadataSourceType = Optional.empty();
        config.filter = Optional.empty();
        config.middleware = Optional.empty();
        config.format = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n";
        config.level = Level.ALL;
        config.disableCertificateValidation = true;
        config.serialization = SplunkHandlerConfig.SerializationFormat.RAW;
        config.url = "http://localhost:8088/";
        //        config.url = splunkInfo.get("quarkus.log.handler.splunk.url");
        AsyncConfig asyncConfig = new AsyncConfig();
        asyncConfig.enable = useAsyncHandler;
        asyncConfig.queueLength = 512;
        asyncConfig.overflow = AsyncHandler.OverflowAction.BLOCK;
        config.async = asyncConfig;
        config.middleware = Optional.of("io.quarkiverse.logging.splunk.LogCounter");
        var optionalRuntimeValue = recorder.initializeHandler(parentConfig, new DiscoveredLogComponents());
        handler = (ExtHandler) optionalRuntimeValue.getValue().get();
        httpServer = ClientAndServer.startClientAndServer(8088);
        // Reject a specific request (ex: wrong token, ...)
        httpServer.when(request())
                .respond(response().withStatusCode(200).withDelay(Delay.delay(TimeUnit.MILLISECONDS, mockDelayMs)));
    }

    @TearDown
    public void tearDown() throws InterruptedException {
        // let readycall to be sent otherwise it creates error logs
        handler.close();
        TimeUnit.SECONDS.sleep(10);
        System.out.println("Number of logs sent " + LogCounter.logSent);
        System.out.println("Number of logs received " + httpServer.retrieveRecordedRequests(null).length);
        httpServer.stop();
    }

    @Benchmark
    @Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
    @Threads(10)
    public int benchLogs() {
        handler.publish(record);
        return hashCode();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .forks(0)
                .jvmArgs("-Xmx256m", "-Xms256m")
                .warmupIterations(0)
                .measurementTime(TimeValue.seconds(5))
                .measurementIterations(1)
                .param("mockDelayMs", "10")

                //                .addProfiler(JavaFlightRecorderProfiler.class, "configName=profile")
                //        .jvmArgs("-Xmx512m", "-Xms512m"
                //            "-Djmh.stack.profiles=" + destinationFolder,
                //            "-Djmh.executor=FJP",
                //            "-Djmh.fr.options=defaultrecording=true,settings=" + profile)
                .include(SplunkHandlerBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
