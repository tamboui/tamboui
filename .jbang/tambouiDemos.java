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
import java.util.Set;
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
 * Utility script to run the demos
 * in a consistent way on Windows, Linux and macOS.
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

    private boolean listOnly;

    public static void main(String... args) {
        int exitCode = new CommandLine(new tambouiDemos()).execute(args);
        exit(exitCode);
    }

    @Override
    public Integer call() {
        return run();
    }

    private ProjectConnection connection;

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
                        .toList();

                // If no demo name provided, list all demos
                if (demoName == null || demoName.isEmpty()) {
                    out.println("Available demos:");
                    demoProjects.forEach(prj -> {
                        String name = prj.getName();
                        String description = prj.getDescription() != null ? prj.getDescription() : "";
                        out.println("  " + name + (description.isEmpty() ? "" : " - " + description));
                    });
                    out.println("\nUse: jbang tambouiDemos.java <demo-name> [--native]");
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
            // e.printStackTrace();
            return 1;
        }
    }

    void runCommand(String command) {
        out.println(command);
        try {
            var isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
            ProcessBuilder processBuilder;

            if (isWindows && command.toLowerCase().endsWith(".bat")) {
                // On Windows, .bat files need to be executed via cmd.exe
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                // .exe files and Unix scripts can be executed directly
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

    void printProject(GradleProject project, String indent, Set<GradleProject> visited) {

        if (visited.contains(project)) {
            return; // Avoid cycles
        }
        visited.add(project);

        out.println(indent + project.getPath());

        // Print children
        for (GradleProject child : project.getChildren()) {
            printProject(child, indent + "  ", visited);
        }
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
            // Stop if we've reached the filesystem root (parent is null or same as current)
            if (parent == null || parent.equals(current)) {
                break;
            }
            current = parent;
        }

        return null;
    }

    /**
     * Run multiple Gradle build tasks
     * Output will be similar to running gradle from the command line
     * 
     * @param taskNames Array of task names to run
     */
    void runBuildTasks(String... taskNames) {
        try {
            BuildLauncher build = connection.newBuild();

            build.withArguments("--quiet");

            // Configure the build with multiple tasks
            build.forTasks(taskNames);

            // Don't set output streams - let Gradle use default System.out/err for normal
            // output
            // Don't add progress listener - it makes output too verbose
            build.setStandardOutput(out);
            build.setStandardError(err);
            // Run the build
            build.run();

        } catch (Exception e) {
            err.println("Error running tasks: " + e.getMessage());
            // e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}