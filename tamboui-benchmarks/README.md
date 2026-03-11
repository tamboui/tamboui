# TamboUI Benchmarks

JMH benchmarks and profiling tools for TamboUI's rendering pipeline.

## Running JMH Benchmarks

```bash
# Run all benchmarks
./gradlew :tamboui-benchmarks:jmh

# Run a specific benchmark
./gradlew :tamboui-benchmarks:jmh -Pjmh.includes='.*sparklineFrame.*'

# Use perfasm for hot assembly analysis (cannot combine with perfnorm)
./gradlew :tamboui-benchmarks:jmh -Pjmh.prof='perfasm'
```

Results are saved to `build/reports/jmh/results.json`.

### Available Benchmarks

| Benchmark | What it measures |
|---|---|
| `SparklineDemoBenchmark` | Full pipeline: widgets → buffer diff → backend draw |
| `RenderPipelineBenchmark` | Buffer operations: setStyle, setString, diff |
| `BackendDrawBenchmark` | Backend draw path with different diff patterns |

## SparklineProfiler

Standalone tool for end-to-end FPS measurement and async-profiler attachment.

### Real Terminal Mode

Renders a sparkline dashboard at max FPS for 10 seconds. FPS is shown in the footer.

```bash
java --enable-native-access=ALL-UNNAMED \
  -cp "$(./gradlew -q :tamboui-benchmarks:sparklineClasspath 2>/dev/null | tail -1)" \
  dev.tamboui.benchmarks.SparklineProfiler
```

```
Total: 5685 frames in 10s, 1759.7 us/frame, 568 FPS
```

### Null Backend Mode

No terminal required. Uses a volatile-sink backend for profiling.

```bash
java -cp "$(./gradlew -q :tamboui-benchmarks:sparklineClasspath 2>/dev/null | tail -1)" \
  dev.tamboui.benchmarks.SparklineProfiler --null [width] [height] [frames]
```

```
Done: 261.6 us/frame, 3822 FPS (sink=27435918)
```

To attach async-profiler, use `-agentpath` at startup:

```bash
java -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints \
  -agentpath:/path/to/libasyncProfiler.so=start,event=cpu,collapsed,file=profile.txt \
  -cp "..." dev.tamboui.benchmarks.SparklineProfiler --null 160 50 300000
```

## Fat JAR

```bash
./gradlew :tamboui-benchmarks:jmhJar
java -jar build/libs/tamboui-benchmarks-*-jmh.jar
```
