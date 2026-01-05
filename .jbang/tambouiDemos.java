///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//REPOS mavenCentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS info.picocli:picocli:4.7.6
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Utility script to run TamboUI demos in a consistent way on Windows, Linux and macOS.
 * <p>
 * Uses the Gradle Tooling API to discover and run demo projects.
 */
@Command(
    name = "tambouiDemos",
    description = "Utility script to run TamboUI demos in a consistent way on Windows, Linux and macOS",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class tambouiDemos implements Callable<Integer> {

    @Parameters(
        index = "0",
        arity = "0..1",
        description = "Name of the demo to run (e.g., jtop-demo, basic-demo). If omitted, lists all available demos."
    )
    private String demoName;

    @Option(
        names = {"-n", "--native"},
        description = "Run the demo as a native executable instead of JVM"
    )
    private boolean nativeMode;

    private ProjectConnection connection;

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new tambouiDemos()).execute(args);
        exit(exitCode);
    }

    @Override
    public Integer call() {
        return run();
    }

    int run() {
        File startDir = new File(".");

        if (!startDir.exists() || !startDir.isDirectory()) {
            err.println("Error: Directory does not exist: " + startDir);
            return 1;
        }

        // Search for Gradle project root
        File projectDir = findGradleProjectRoot(startDir);
        if (projectDir == null) {
            err.println("Error: Not a Gradle project (no build.gradle.kts or settings.gradle.kts found)");
            err.println("Searched from: " + startDir.getAbsolutePath());
            return 1;
        }

        out.println("Finding demos in: " + projectDir.getAbsolutePath());
        try {
            connection = GradleConnector.newConnector()
                    .forProjectDirectory(projectDir)
                    .connect();

            try {
                // Get the Gradle project model which contains all projects
                GradleProject rootProject = connection.getModel(GradleProject.class);

                List<GradleProject> projects = new ArrayList<>();
                collectProjects(rootProject, projects);

                List<GradleProject> demoProjects = projects.stream()
                        .filter(project -> project.getPath().contains(":demos:"))
                        .filter(project -> !project.getName().equals("demo-selector"))
                        .toList();

                // If no demo name provided, list all demos
                if (demoName == null || demoName.isEmpty()) {
                    out.println("Available demos:");
                    demoProjects.forEach(prj -> {
                        String name = prj.getName();
                        String description = prj.getDescription() != null ? prj.getDescription() : "";
                        String module = extractModule(prj.getPath());
                        out.println("  " + name + " (" + module + ")" + (description.isEmpty() ? "" : " - " + description));
                    });
                    out.println("\nUse: jbang run-demo <demo-name> [--native]");
                    return 0;
                }

                // Find the requested demo
                var demoProject = demoProjects.stream()
                        .filter(prj -> prj.getName().equals(demoName) || prj.getName().contains(demoName))
                        .findFirst()
                        .orElse(null);

                if (demoProject == null) {
                    err.println("Error: Demo '" + demoName + "' not found.");
                    err.println("Available demos:");
                    demoProjects.forEach(prj -> err.println("  " + prj.getName()));
                    return 1;
                }

                // Run the demo
                if (nativeMode) {
                    runNative(demoProject);
                } else {
                    runJVM(demoProject);
                }

                return 0;

            } finally {
                connection.close();
            }
        } catch (Exception e) {
            err.println("Error connecting to Gradle build: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Extracts the module name from a Gradle project path.
     *
     * @param path the project path (e.g., ":tamboui-widgets:demos:basic-demo")
     * @return the module name (e.g., "tamboui-widgets")
     */
    String extractModule(String path) {
        // Path format: :module:demos:demo-name or :demos:demo-name
        if (path.startsWith(":demos:")) {
            return "root";
        }
        String[] parts = path.split(":");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "unknown";
    }

    void runCommand(String command) {
        out.println(command);
        try {
            var isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
            ProcessBuilder processBuilder;

            if (isWindows && command.toLowerCase().endsWith(".bat")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                processBuilder = new ProcessBuilder(command);
            }

            var process = processBuilder.inheritIO().start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            err.println("Error running " + command + ": " + e.getMessage());
        }
    }

    boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    void runJVM(GradleProject project) {
        out.println("Building " + project.getPath() + "...");
        runBuildTasks(project.getPath() + ":installDist");
        out.println("");
        out.println("Running " + project.getPath() + "...");
        var scriptName = project.getName() + (isWindows() ? ".bat" : "");
        var command = project.getProjectDirectory().toPath()
                .resolve("build/install/" + project.getName() + "/bin/" + scriptName);

        runCommand(command.toString());
    }

    void runNative(GradleProject project) {
        out.println("Building " + project.getPath() + " for native...");
        runBuildTasks(project.getPath() + ":nativeCompile");
        out.println("");
        out.println("Running " + project.getPath() + " (native)...");
        var exeName = project.getName() + (isWindows() ? ".exe" : "");
        var command = project.getProjectDirectory().toPath().resolve("build/native/nativeCompile/" + exeName);

        runCommand(command.toString());
    }

    void collectProjects(GradleProject project, List<GradleProject> paths) {
        paths.add(project);
        for (GradleProject child : project.getChildren()) {
            collectProjects(child, paths);
        }
    }

    File findGradleProjectRoot(File startDir) {
        File current = startDir.getAbsoluteFile();

        while (current != null) {
            File buildFile = new File(current, "build.gradle.kts");
            File settingsFile = new File(current, "settings.gradle.kts");
            File buildFileGroovy = new File(current, "build.gradle");
            File settingsFileGroovy = new File(current, "settings.gradle");

            if (buildFile.exists() || settingsFile.exists() ||
                    buildFileGroovy.exists() || settingsFileGroovy.exists()) {
                return current;
            }

            File parent = current.getParentFile();
            if (parent == null || parent.equals(current)) {
                break;
            }
            current = parent;
        }

        return null;
    }

    /**
     * Run Gradle build tasks.
     *
     * @param taskNames the tasks to run
     */
    void runBuildTasks(String... taskNames) {
        try {
            BuildLauncher build = connection.newBuild();
            build.withArguments("--quiet");
            build.forTasks(taskNames);
            build.setStandardOutput(out);
            build.setStandardError(err);
            build.run();
        } catch (Exception e) {
            err.println("Error running tasks: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
