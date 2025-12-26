///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS dev.jbang:jash:LATEST

import static dev.jbang.jash.Jash.start;
import static java.lang.IO.println;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.size;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Arrays;

String formatFileSize(long bytes) {
    if (bytes < 1024) {
        return bytes + " B";
    } else if (bytes < 1024 * 1024) {
        return String.format("%.1f KB", bytes / 1024.0);
    } else {
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}

Path findRepoRoot(Path startDir) {
    Path current = startDir.toAbsolutePath().normalize();
    while (current != null) {
        Path gitDir = current.resolve(".git");
        if (exists(gitDir)) {
            return current;
        }
        Path parent = current.getParent();
        if (parent == null || parent.equals(current)) {
            break; // Reached filesystem root
        }
        current = parent;
    }
    return null; // Not found
}

String getSourceUrl(String baseName, Path repoRoot) {
    try {
        ProcessBuilder pb = new ProcessBuilder("jbang", "info", "tools", "--select", "originalResource", baseName);
        pb.directory(repoRoot.toFile());
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        if (process.waitFor() == 0 && !output.isEmpty() && !output.startsWith("Error")) {
            Path sourceFile = Paths.get(output);
            if (sourceFile.isAbsolute() && sourceFile.startsWith(repoRoot)) {
                Path relativePath = repoRoot.relativize(sourceFile);
                String sourcePath = relativePath.toString().replace('\\', '/');
                return "https://github.com/tamboui/tamboui/blob/main/" + sourcePath;
            } else if (!sourceFile.isAbsolute()) {
                String sourcePath = sourceFile.toString().replace('\\', '/');
                return "https://github.com/tamboui/tamboui/blob/main/" + sourcePath;
            }
        }
    } catch (Exception e) {
        // Ignore errors
    }
    return null;
}

void generateReadme(Path outputDir, Map<String, List<Path>> filesByBaseName, Path repoRoot) throws IOException {
    Path readmeFile = outputDir.resolve("README.md");
    try (var writer = Files.newBufferedWriter(readmeFile)) {
        writer.write("# TamboUI Demo Videos\n\n");
        writer.write("This directory contains demo videos showcasing TamboUI widgets and features.\n\n");
        writer.write("## Demos\n\n");
        
        List<String> videoExtensions = List.of("mp4", "webm", "ogv", "mov", "avi", "mkv");
        
        filesByBaseName.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String baseName = entry.getKey();
                List<Path> variants = entry.getValue();
                String sourceUrl = getSourceUrl(baseName, repoRoot);
                
                try {
                    // Find preview image (prefer SVG, otherwise first non-video file)
                    Path previewImage = variants.stream()
                        .filter(f -> {
                            String fileName = f.getFileName().toString();
                            String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                            return !videoExtensions.contains(ext);
                        })
                        .filter(f -> f.getFileName().toString().endsWith(".svg"))
                        .findFirst()
                        .orElse(variants.stream()
                            .filter(f -> {
                                String fileName = f.getFileName().toString();
                                String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                                return !videoExtensions.contains(ext);
                            })
                            .findFirst()
                            .orElse(variants.get(0)));
                    
                    String previewName = previewImage.getFileName().toString();
                    String ext = previewName.substring(previewName.lastIndexOf('.') + 1).toLowerCase();
                    boolean isVideo = videoExtensions.contains(ext);
                    
                    writer.write(String.format("### %s\n\n", baseName));
                    
                    // Add preview image/video
                    if (isVideo) {
                        writer.write(String.format("<video src=\"%s\" controls width=\"600\"></video>\n\n", previewName));
                    } else {
                        writer.write(String.format("![%s](%s)\n\n", baseName, previewName));
                    }
                    
                    // Add source link if available
                    if (sourceUrl != null) {
                        writer.write(String.format("**Source:** [%s](%s)\n\n", baseName, sourceUrl));
                    }
                    
                    // Add links to all variants
                    if (variants.size() > 1) {
                        writer.write("**Formats:** ");
                        List<String> formatLinks = new ArrayList<>();
                        for (Path variant : variants) {
                            String variantName = variant.getFileName().toString();
                            String variantExt = variantName.substring(variantName.lastIndexOf('.') + 1);
                            formatLinks.add(String.format("[%s](%s)", variantExt.toUpperCase(), variantName));
                        }
                        writer.write(String.join(" | ", formatLinks));
                        writer.write("\n\n");
                    }
                    
                    writer.write("---\n\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        
        writer.write("## View All Videos\n\n");
        writer.write("Open [index.html](index.html) in a browser to view all demo videos in a gallery.\n");
    }
}

void main(String... args) throws IOException {
    println("Generating video...");

    Path videosDir = Path.of(".").toAbsolutePath().normalize();
    
    // Find repo root by looking for .git folder
    final Path repoRoot = findRepoRoot(videosDir);
    if (repoRoot == null) {
        System.err.println("Error: Could not find .git folder. Please run this script from within the tamboui repository.");
        System.exit(1);
    }

    Path outputDir = videosDir.resolve("output");

    // Parse output formats from arguments (default to svg if none provided)
    List<String> outputFormats = new ArrayList<>();
    if (args.length > 0) {
        for (String arg : args) {
            // Treat arguments as format extensions if they look like simple alphanumeric strings
            if (arg.matches("^[a-zA-Z0-9]+$") && arg.length() < 20) {
                outputFormats.add(arg);
            }
        }
    }
    if (outputFormats.isEmpty()) {
        outputFormats.add("svg"); // default format
    }

    String pattern = ".*\\.tape$";
    Pattern filter = Pattern.compile(pattern);

    for (Path path : Files.list(videosDir).toList()) {
        if (filter.matcher(path.getFileName().toString()).matches()) {
            if(path.getFileName().toString().endsWith("_.tape")) {
                continue; // ignore shared tapes
            }
            println("Processing video: " + path.getFileName());

            String baseName = path.getFileName().toString().replace(".tape", "");
            List<Path> outputPaths = new ArrayList<>();
            for (String format : outputFormats) {
                outputPaths.add(outputDir.resolve(baseName + "." + format));
            }

            // Only generate if all output paths are newer than input path
            // Also check if shared_.tape is newer than the outputs, to handle dependency on shared config/tape
            Path sharedTape = videosDir.resolve("shared_.tape");
            boolean upToDate = true;
            for (Path outputPath : outputPaths) {
                if (!exists(outputPath)) {
                    upToDate = false;
                    break;
                }
                if (getLastModifiedTime(outputPath).compareTo(getLastModifiedTime(path)) < 0) {
                    upToDate = false;
                    break;
                }
                if (exists(sharedTape)) {
                    if (getLastModifiedTime(outputPath).compareTo(getLastModifiedTime(sharedTape)) < 0) {
                        upToDate = false;
                        break;
                    }
                }
            }
            if (upToDate) {
                println("  (up to date: skipping)");
                continue;
            }

            // Generate all output formats in a single command with multiple -o flags
            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.add("vhs");
            for (Path outputPath : outputPaths) {
                cmdArgs.add("-o");
                cmdArgs.add(outputPath.toString());
            }
            cmdArgs.add(path.toString());
            
            // Unpack the list to pass as varargs
            String[] argsArray = cmdArgs.toArray(new String[0]);
            start(argsArray[0], Arrays.copyOfRange(argsArray, 1, argsArray.length))
                    .stream()
                    .peek(System.out::println)
                    .count();
            println("Video processed: " + path.getFileName());
        }
    }

    Path htmlOutputDir = outputDir;
    Files.createDirectories(htmlOutputDir); // make sure it exists

    println("Generating index.html...");

    // Generate index.html showing all SVGs in outputDir
    Path indexFile = htmlOutputDir.resolve("index.html");
    try (var writer = Files.newBufferedWriter(indexFile)) {
        writer.write(
                """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <title>Demo Videos</title>
                            <style>
                                body { font-family: sans-serif; background: #222; color: #fafafa; margin: 2em; }
                                .gallery { display: flex; flex-wrap: wrap; gap: 2em; }
                                .item { background: #333; padding: 1em; border-radius: 8px; box-shadow: 0 2px 12px #0003; }
                                .item h2 { font-size: 1em; margin-bottom: 0.5em; color: #ffb347; }
                                .item img { width: 600px; max-width: 100vw; background: #222; display: block; cursor: pointer; margin-bottom: 0.5em; }
                                .item .variants { display: flex; flex-wrap: wrap; gap: 0.5em; margin-top: 0.5em; }
                                .item .variants a { color: #4a9eff; text-decoration: none; padding: 0.25em 0.5em; background: #444; border-radius: 4px; font-size: 0.9em; }
                                .item .variants a:hover { background: #555; color: #6bb3ff; }
                                .item .variants a.smallest { background: #2d5a2d; color: #4ade80; border: 1px solid #4ade80; }
                                .item .variants a.smallest:hover { background: #3a6b3a; color: #6ee89e; }
                                .item .variants a.largest { background: #5a2d2d; color: #f87171; border: 1px solid #f87171; }
                                .item .variants a.largest:hover { background: #6b3a3a; color: #fca5a5; }
                                .modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.9); }
                                .modal.active { display: flex; align-items: center; justify-content: center; }
                                .modal-content { max-width: 90vw; max-height: 90vh; background: #222; padding: 2em; border-radius: 8px; position: relative; }
                                .modal-content img { max-width: 100%; max-height: 90vh; display: block; }
                                .modal-content video { max-width: 100%; max-height: 90vh; display: block; }
                                .modal-close { position: absolute; top: 10px; right: 20px; color: #fafafa; font-size: 2em; font-weight: bold; cursor: pointer; line-height: 1; }
                                .modal-close:hover { color: #ffb347; }
                            </style>
                        </head>
                        <body>
                            <h1>TamboUI Demo Videos</h1>
                            <div class="gallery">
                        """);

        // Group files by base name (without extension)
        Map<String, List<Path>> filesByBaseName = new HashMap<>();
        try (var files = Files.list(outputDir)) {
            files.filter(f -> {
                    String fileName = f.getFileName().toString();
                    return !fileName.equals("index.html") && !fileName.equals("README.md");
                })
                 .sorted()
                 .forEach(file -> {
                String fileName = file.getFileName().toString();
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                    String baseName = fileName.substring(0, lastDot);
                    filesByBaseName.computeIfAbsent(baseName, k -> new ArrayList<>()).add(file);
                }
            });
        }

        println("Generating README.md...");
        generateReadme(htmlOutputDir, filesByBaseName, repoRoot);
                
        // Generate HTML for each group
        filesByBaseName.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String baseName = entry.getKey();
                List<Path> variants = entry.getValue();
                
                // Get source file path using jbang
                String sourceUrl = getSourceUrl(baseName, repoRoot);
                
                // Find preview image (prefer SVG, otherwise first non-video file)
                List<String> videoExtensions = List.of("mp4", "webm", "ogv", "mov", "avi", "mkv");
                Path previewImage = variants.stream()
                    .filter(f -> {
                        String fileName = f.getFileName().toString();
                        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                        return !videoExtensions.contains(ext);
                    })
                    .filter(f -> f.getFileName().toString().endsWith(".svg"))
                    .findFirst()
                    .orElse(variants.stream()
                        .filter(f -> {
                            String fileName = f.getFileName().toString();
                            String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                            return !videoExtensions.contains(ext);
                        })
                        .findFirst()
                        .orElse(variants.get(0)));
                
                try {
                    writer.write("<div class=\"item\">\n");
                    if (sourceUrl != null) {
                        writer.write("  <h2><a href=\"" + sourceUrl + "\" target=\"_blank\" style=\"color: #ffb347; text-decoration: none;\">" + baseName + "</a></h2>\n");
                    } else {
                        writer.write("  <h2>" + baseName + "</h2>\n");
                    }
                    
                    // Show preview image
                    String previewName = previewImage.getFileName().toString();
                    long previewSizeBytes = size(previewImage);
                    String previewSize = formatFileSize(previewSizeBytes);
                    writer.write("  <img src=\"" + previewName + "\" alt=\"" + baseName + "\" title=\"" + previewSize + "\" onclick=\"openModal('" + previewName
                            + "')\">\n");
                    
                    // Calculate smallest and largest file sizes for highlighting
                    long smallestSize = variants.stream()
                        .mapToLong(v -> {
                            try {
                                return size(v);
                            } catch (IOException e) {
                                return Long.MAX_VALUE;
                            }
                        })
                        .min()
                        .orElse(1);
                    
                    long largestSize = variants.stream()
                        .mapToLong(v -> {
                            try {
                                return size(v);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .max()
                        .orElse(1);
                    
                    // Show all variants as links
                    writer.write("  <div class=\"variants\">\n");
                    for (Path variant : variants) {
                        String variantName = variant.getFileName().toString();
                        String extension = variantName.substring(variantName.lastIndexOf('.') + 1);
                        long variantSizeBytes = size(variant);
                        String variantSize = formatFileSize(variantSizeBytes);
                        double percentage = (variantSizeBytes * 100.0) / largestSize;
                        String percentageStr = String.format("%.0f%%", percentage);
                        
                        // Determine CSS class based on size
                        String cssClass = "";
                        if (variantSizeBytes == smallestSize && smallestSize != largestSize) {
                            cssClass = " class=\"smallest\"";
                        } else if (variantSizeBytes == largestSize && smallestSize != largestSize) {
                            cssClass = " class=\"largest\"";
                        }
                        
                        writer.write("    <a href=\"" + variantName + "\"" + cssClass + " onclick=\"event.preventDefault(); openModal('" + variantName + "');\">" 
                                + extension.toUpperCase() + " (" + variantSize + ", " + percentageStr + ")</a>\n");
                    }
                    writer.write("  </div>\n");
                    writer.write("</div>\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        writer.write("""
                    </div>
                    <div id="modal" class="modal" onclick="closeModal()">
                        <div class="modal-content" onclick="event.stopPropagation()">
                            <span class="modal-close" onclick="closeModal()">&times;</span>
                            <img id="modal-img" src="" alt="" style="display: none;">
                            <video id="modal-video" controls style="display: none;"></video>
                        </div>
                    </div>
                    <script>
                        function openModal(fileSrc) {
                            const img = document.getElementById('modal-img');
                            const video = document.getElementById('modal-video');
                            const modal = document.getElementById('modal');
                            
                            // Check if it's a video file
                            const videoExtensions = ['mp4', 'webm', 'ogv', 'mov', 'avi', 'mkv'];
                            const extension = fileSrc.split('.').pop().toLowerCase();
                            const isVideo = videoExtensions.includes(extension);
                            
                            if (isVideo) {
                                img.style.display = 'none';
                                video.style.display = 'block';
                                video.src = fileSrc;
                                video.load();
                            } else {
                                video.style.display = 'none';
                                img.style.display = 'block';
                                img.src = fileSrc;
                            }
                            
                            modal.classList.add('active');
                        }
                        function closeModal() {
                            const video = document.getElementById('modal-video');
                            video.pause();
                            video.currentTime = 0;
                            document.getElementById('modal').classList.remove('active');
                        }
                        document.addEventListener('keydown', function(e) {
                            if (e.key === 'Escape') {
                                closeModal();
                            }
                        });
                    </script>
                </body>
                </html>
                """);
    }
}
