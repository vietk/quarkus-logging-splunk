# JMH Benchmark module

This module attempt to benchmark the splunk log handler provided by this project:

- The initialization of the benchmark sets up a log handler with default parameters
- The benchmark has some parameters, namely
    - useAsyncHandler(true/false): wraps the splunk log handler in an async logger instance
    - mockDelayMs(long): allow to establish a delay of the response of the Splunk mocked server
    - hecSendMode(PARALLEL/SEQUENTIAL): configure the way the log are sent to the Splunk instance

## How to use? 

```shell

# simple benchmark call
java -jar benchmarks/target/benchmarks.jar -f1 -t1 -pmockDelayMs=delday -puseAsyncHandler=true -phecSendMode=PARALLEL

#  benchmark call with profiler
java -jar benchmarks/target/benchmarks.jar \
      -f1 -t250 -r30 \
      -pmockDelayMs=100 -puseAsyncHandler=true -phecSendMode=PARALLEL \
      -prof "jfr:dir=your-dir-$(date +%Y%m%d-%H%M);verbose=true" \
      -jvmArgs="-Xmx256m -Xms256m"
```
  