plugins {
    id("dev.tamboui.java-library")
    id("me.champeau.jmh") version "0.7.2"
}

description = "Performance benchmarks for TamboUI using JMH"

dependencies {
    jmh(project(":tamboui-core"))
    jmh(project(":tamboui-widgets"))
    jmh(project(":tamboui-jline3-backend"))
    jmh(project(":tamboui-panama-backend"))
}

jmh {
    // JMH configuration
    iterations = 3  // Number of measurement iterations
    warmupIterations = 2  // Number of warmup iterations
    fork = 2  // Number of forked JVMs
    benchmarkMode = listOf("avgt")  // Average time
    timeUnit = "us"  // Microseconds

    // Profilers: perfnorm shows CPU counters, gc shows allocation rate
    profilers = listOf("perfnorm", "gc")

    // JVM arguments for the benchmark
    jvmArgs = listOf("-Xms2g", "-Xmx2g")

    // Output format
    resultFormat = "JSON"
    resultsFile = project.file("${project.layout.buildDirectory.get()}/reports/jmh/results.json")
}

tasks {
    // Ensure we can run benchmarks with: ./gradlew jmh
    named("jmh") {
        group = "benchmark"
        description = "Run JMH benchmarks"
    }

    register("sparklineClasspath") {
        group = "benchmark"
        description = "Print classpath for SparklineProfiler"
        dependsOn("jmhClasses")
        doLast {
            println(sourceSets["jmh"].runtimeClasspath.asPath)
        }
    }
}
