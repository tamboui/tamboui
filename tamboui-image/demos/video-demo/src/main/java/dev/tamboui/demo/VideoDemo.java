///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-image:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
//JAVA_OPTIONS -Dapple.awt.UIElement=true
//JAVA_OPTIONS --enable-native-access=ALL-UNNAMED

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.image.Image;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.ImageScaling;
import dev.tamboui.image.capability.TerminalImageCapabilities;
import dev.tamboui.image.protocol.BrailleProtocol;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.image.protocol.ImageProtocol;
import dev.tamboui.image.protocol.ITermProtocol;
import dev.tamboui.image.protocol.KittyProtocol;
import dev.tamboui.image.protocol.SixelProtocol;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demo showcasing video playback by decoding frames via ffmpeg and streaming them into {@link Image}.
 * <p>
 * Requirements:
 * <ul>
 *   <li>{@code ffmpeg} available on PATH</li>
 *   <li>(Optional) {@code ffplay} for audio playback</li>
 * </ul>
 */
public class VideoDemo {

    private static final ImageProtocol HALF_BLOCK = new HalfBlockProtocol();
    private static final ImageProtocol BRAILLE = new BrailleProtocol();
    private static final ImageProtocol SIXEL = new SixelProtocol();
    private static final ImageProtocol KITTY = new KittyProtocol();
    private static final ImageProtocol ITERM2 = new ITermProtocol();

    private static final int DEFAULT_FPS = 15;
    private static final double SEEK_SECONDS = 5.0;

    private boolean running = true;
    private final TerminalImageCapabilities capabilities;
    private ImageProtocol currentProtocol;
    private ImageScaling currentScaling = ImageScaling.FIT;
    private boolean forceProtocol;

    private final VideoSource source;
    private final FfmpegPlayer player;

    public static void main(String[] args) throws Exception {
        new VideoDemo(parseArgs(args)).run();
    }

    private VideoDemo(VideoSource source) throws IOException {
        this.source = source;
        this.capabilities = TerminalImageCapabilities.detect();
        this.currentProtocol = capabilities.bestProtocol();
        this.player = new FfmpegPlayer(source, DEFAULT_FPS);
    }

    private void run() throws Exception {
        try (var backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            var terminal = new Terminal<>(backend);

            backend.onResize(player::requestRestart);
            player.play();

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(1000 / DEFAULT_FPS);
                if (c != -2) {
                    handleInput(c);
                }
            }
        } finally {
            player.close();
        }
    }

    private void handleInput(int c) {
        var previousProtocol = currentProtocol;

        switch (c) {
            case 'q':
            case 'Q':
            case 3: // Ctrl+C
                running = false;
                break;
            case ' ': // play/pause
            case 'p':
            case 'P':
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
                break;
            case 'r':
            case 'R':
                player.seekTo(0.0);
                break;
            case 'h': // back
            case 'H':
                player.seekBy(-SEEK_SECONDS);
                break;
            case 'l': // forward
            case 'L':
                player.seekBy(SEEK_SECONDS);
                break;
            case 'm': // toggle audio
            case 'M':
                player.toggleAudio();
                break;
            case '1':
                currentProtocol = HALF_BLOCK;
                break;
            case '2':
                currentProtocol = BRAILLE;
                break;
            case '3':
                currentProtocol = SIXEL;
                break;
            case '4':
                currentProtocol = KITTY;
                break;
            case '5':
                currentProtocol = ITERM2;
                break;
            case 'a':
            case 'A':
                currentProtocol = capabilities.bestProtocol();
                break;
            case 'f':
                currentScaling = ImageScaling.FIT;
                break;
            case 'i':
                currentScaling = ImageScaling.FILL;
                break;
            case 's':
                currentScaling = ImageScaling.STRETCH;
                break;
            case 'n':
                currentScaling = ImageScaling.NONE;
                break;
            case 6: // Ctrl+F - force protocol
                forceProtocol = true;
                break;
            default:
                break;
        }

        if (previousProtocol != currentProtocol) {
            forceProtocol = false;
        }
    }

    private boolean isCurrentProtocolSupported() {
        return capabilities.supports(currentProtocol.protocolType());
    }

    private void ui(Frame frame) {
        var area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),   // Header
                Constraint.length(4),   // Status
                Constraint.fill(),      // Video
                Constraint.length(4)    // Footer/help
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderStatus(frame, layout.get(1));
        renderVideo(frame, layout.get(2));
        renderFooter(frame, layout.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        var headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Video Demo ").yellow(),
                    Span.raw(player.isPlaying() ? "▶" : "⏸").bold().green()
                )
            ).centered())
            .build();
        frame.renderWidget(headerBlock, area);
    }

    private void renderStatus(Frame frame, Rect area) {
        var src = source.displayName();
        var proto = currentProtocol.name();
        var protoSupport = isCurrentProtocolSupported() ? Span.raw("supported").green() : Span.raw("not detected").yellow();

        var line1 = Line.from(
            Span.raw(" Source: ").dim(),
            Span.raw(src).bold().cyan()
        );

        var line2 = Line.from(
            Span.raw(" Protocol: ").dim(),
            Span.raw(proto).bold().yellow(),
            Span.raw(" (").dim(),
            protoSupport,
            Span.raw(")").dim(),
            Span.raw("  Scaling: ").dim(),
            Span.raw(currentScaling.name()).bold().magenta()
        );

        var line3 = Line.from(
            Span.raw(" Time: ").dim(),
            Span.raw(formatSeconds(player.positionSeconds())).bold().white(),
            Span.raw("  FPS: ").dim(),
            Span.raw(String.valueOf(player.fps())).bold().white(),
            Span.raw("  Frames: ").dim(),
            Span.raw(String.valueOf(player.framesDecoded())).bold().white(),
            Span.raw("  Dropped: ").dim(),
            Span.raw(String.valueOf(player.framesDroppedEstimate())).bold().white(),
            Span.raw("  Audio: ").dim(),
            Span.raw(player.audioEnabled() ? "on" : "off").bold().white()
        );

        var paragraph = Paragraph.builder()
            .text(Text.from(line1, line2, line3))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderVideo(Frame frame, Rect area) {
        if (!isCurrentProtocolSupported() && !forceProtocol) {
            renderUnsupportedProtocolWarning(frame, area);
            return;
        }

        var videoBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.BLUE))
            .title(Title.from(Line.from(Span.raw(" Video ").blue())))
            .build();

        frame.renderWidget(videoBlock, area);
        var inner = videoBlock.inner(area);
        if (inner.isEmpty()) {
            return;
        }

        var res = currentProtocol.resolution();
        int targetW = Math.max(1, inner.width() * res.widthMultiplier());
        int targetH = Math.max(1, inner.height() * res.heightMultiplier());
        player.setTargetSize(targetW, targetH);

        var img = player.latestFrame();
        if (img == null) {
            var msg = player.errorMessage();
            var lines = new ArrayList<Line>();
            lines.add(Line.empty());
            lines.add(Line.from(Span.raw("Waiting for frames...").dim()));
            if (msg != null && !msg.isEmpty()) {
                lines.add(Line.empty());
                lines.add(Line.from(Span.raw(msg).red()));
            }
            var wait = Paragraph.builder()
                .text(Text.from(lines))
                .centered()
                .build();
            frame.renderWidget(wait, inner);
            return;
        }

        var image = Image.builder()
            .data(img)
            .scaling(currentScaling)
            .protocol(currentProtocol)
            .build();

        frame.renderWidget(image, inner);
    }

    private void renderUnsupportedProtocolWarning(Frame frame, Rect area) {
        var warningLine1 = Line.from(
            Span.raw("The ").dim(),
            Span.raw(currentProtocol.name()).bold().yellow(),
            Span.raw(" protocol is not detected as supported by your terminal.").dim()
        );
        var warningLine2 = Line.from(
            Span.raw("Press ").dim(),
            Span.raw("Ctrl+F").bold().cyan(),
            Span.raw(" to force using it anyway.").dim()
        );

        var warning = Paragraph.builder()
            .text(Text.from(Line.empty(), warningLine1, Line.empty(), warningLine2))
            .centered()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(Span.raw(" Video ").blue())))
                .build())
            .build();

        frame.renderWidget(warning, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        var helpLine1 = Line.from(
            Span.raw(" Playback: ").dim(),
            Span.raw("Space").bold().yellow(),
            Span.raw(" Play/Pause  ").dim(),
            Span.raw("h").bold().yellow(),
            Span.raw(" -5s  ").dim(),
            Span.raw("l").bold().yellow(),
            Span.raw(" +5s  ").dim(),
            Span.raw("r").bold().yellow(),
            Span.raw(" Restart  ").dim(),
            Span.raw("m").bold().yellow(),
            Span.raw(" Audio  ").dim(),
            Span.raw("q").bold().red(),
            Span.raw(" Quit").dim()
        );

        var helpLine2 = Line.from(
            Span.raw(" Protocol: ").dim(),
            Span.raw("1").bold().yellow(),
            Span.raw(" Half-Block ").dim(),
            Span.raw("2").bold().yellow(),
            Span.raw(" Braille ").dim(),
            Span.raw("3").bold().yellow(),
            Span.raw(" Sixel ").dim(),
            Span.raw("4").bold().yellow(),
            Span.raw(" Kitty ").dim(),
            Span.raw("5").bold().yellow(),
            Span.raw(" iTerm2 ").dim(),
            Span.raw("a").bold().yellow(),
            Span.raw(" Auto").dim()
        );

        var footer = Paragraph.builder()
            .text(Text.from(helpLine1, helpLine2))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }

    private static String formatSeconds(double seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long s = (long) seconds;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec);
        }
        return String.format(Locale.ROOT, "%d:%02d", m, sec);
    }

    private static VideoSource parseArgs(String[] args) {
        boolean audio = false;
        String input = null;

        for (var arg : args) {
            if ("--audio".equals(arg)) {
                audio = true;
            } else if ("--no-audio".equals(arg)) {
                audio = false;
            } else if ("--testsrc".equals(arg)) {
                input = null;
            } else if (!arg.startsWith("-") && input == null) {
                input = arg;
            }
        }

        return new VideoSource(input, audio);
    }

    private static final class VideoSource {
        private final String input;
        private final boolean audio;

        private VideoSource(String input, boolean audio) {
            this.input = input;
            this.audio = audio;
        }

        boolean isTestSrc() {
            return input == null || input.trim().isEmpty();
        }

        boolean audioEnabledByDefault() {
            return audio;
        }

        String displayName() {
            if (isTestSrc()) {
                return "testsrc (generated)";
            }
            return Path.of(input).getFileName().toString();
        }

        String input() {
            return input;
        }
    }

    /**
     * Background ffmpeg decoder that always keeps only the latest frame (drop-frames strategy).
     */
    private static final class FfmpegPlayer implements AutoCloseable {
        private final Object lock = new Object();
        private final VideoSource source;
        private final int fps;

        private volatile int targetWidth = 0;
        private volatile int targetHeight = 0;
        private volatile boolean restartRequested = false;

        private volatile boolean playing = false;
        private volatile boolean audioEnabled;

        private volatile double positionSeconds = 0.0;
        private volatile long decodedFrames = 0;
        private volatile long presentedFrames = 0;
        private volatile long lastPresentedSeq = -1;
        private volatile String errorMessage = null;

        private final AtomicReference<DecodedFrame> latest = new AtomicReference<>();

        private Process ffmpeg;
        private Thread decodeThread;

        private Process ffplay;

        FfmpegPlayer(VideoSource source, int fps) {
            this.source = source;
            this.fps = fps;
            this.audioEnabled = source.audioEnabledByDefault();
        }

        int fps() {
            return fps;
        }

        boolean isPlaying() {
            return playing;
        }

        boolean audioEnabled() {
            return audioEnabled;
        }

        double positionSeconds() {
            return positionSeconds;
        }

        long framesDecoded() {
            return decodedFrames;
        }

        long framesDroppedEstimate() {
            long dropped = decodedFrames - presentedFrames;
            return Math.max(0, dropped);
        }

        ImageData latestFrame() {
            var frame = latest.get();
            if (frame != null && frame.seq != lastPresentedSeq) {
                lastPresentedSeq = frame.seq;
                presentedFrames++;
            }
            return frame != null ? frame.data : null;
        }

        String errorMessage() {
            return errorMessage;
        }

        void toggleAudio() {
            audioEnabled = !audioEnabled;
            if (playing) {
                requestRestart();
            }
        }

        void setTargetSize(int width, int height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            if (width == targetWidth && height == targetHeight) {
                return;
            }
            targetWidth = width;
            targetHeight = height;
            if (playing) {
                requestRestart();
            }
        }

        void requestRestart() {
            restartRequested = true;
        }

        void play() {
            synchronized (lock) {
                if (playing) {
                    return;
                }
                playing = true;
                restartRequested = true;
                ensureRunningLocked();
            }
        }

        void pause() {
            synchronized (lock) {
                if (!playing) {
                    return;
                }
                playing = false;
                stopProcessesLocked();
            }
        }

        void seekBy(double deltaSeconds) {
            seekTo(positionSeconds + deltaSeconds);
        }

        void seekTo(double seconds) {
            if (seconds < 0) {
                seconds = 0;
            }
            positionSeconds = seconds;
            if (playing) {
                requestRestart();
            }
        }

        @Override
        public void close() {
            synchronized (lock) {
                playing = false;
                stopProcessesLocked();
            }
        }

        private void ensureRunningLocked() {
            if (decodeThread == null) {
                decodeThread = new Thread(this::decodeLoop, "video-demo-ffmpeg-decode");
                decodeThread.setDaemon(true);
                decodeThread.start();
            }
        }

        private void stopProcessesLocked() {
            if (ffmpeg != null) {
                ffmpeg.destroy();
                ffmpeg = null;
            }
            if (ffplay != null) {
                ffplay.destroy();
                ffplay = null;
            }
        }

        private void decodeLoop() {
            while (true) {
                if (!playing) {
                    // Fully stop thread when not playing; play() will recreate it.
                    synchronized (lock) {
                        decodeThread = null;
                        return;
                    }
                }

                if (restartRequested) {
                    restartDecoder();
                    restartRequested = false;
                }

                // If we don't have a valid target yet, wait a bit.
                if (targetWidth <= 0 || targetHeight <= 0) {
                    sleepQuietly(10);
                    continue;
                }

                // Decoder reads frames; if it exits, we restart (unless paused).
                if (ffmpeg == null) {
                    restartDecoder();
                    sleepQuietly(10);
                    continue;
                }

                if (!ffmpeg.isAlive()) {
                    ffmpeg = null;
                    continue;
                }

                sleepQuietly(5);
            }
        }

        private void restartDecoder() {
            synchronized (lock) {
                stopProcessesLocked();

                try {
                    errorMessage = null;
                    var cmd = ffmpegCommand();
                    ffmpeg = new ProcessBuilder(cmd)
                        .start();

                    if (audioEnabled) {
                        tryStartAudioLocked();
                    }

                    startStderrDrainerThread(ffmpeg.getErrorStream());
                    startFrameReaderThread(ffmpeg.getInputStream(), targetWidth, targetHeight);
                } catch (IOException e) {
                    errorMessage = "Failed to start ffmpeg. Install ffmpeg or check PATH.";
                    ffmpeg = null;
                }
            }
        }

        private void tryStartAudioLocked() {
            if (source.isTestSrc()) {
                return;
            }
            // Best-effort: start ffplay for audio. We keep it simple: restart on pause/seek.
            var cmd = new ArrayList<String>();
            cmd.add("ffplay");
            cmd.add("-hide_banner");
            cmd.add("-loglevel");
            cmd.add("error");
            cmd.add("-nodisp");
            cmd.add("-autoexit");
            cmd.add("-ss");
            cmd.add(String.format(Locale.ROOT, "%.3f", positionSeconds));
            cmd.add(source.input());
            try {
                ffplay = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            } catch (IOException ignored) {
                // Audio is optional; don't fail demo.
            }
        }

        private List<String> ffmpegCommand() {
            int w = Math.max(2, targetWidth);
            int h = Math.max(2, targetHeight);

            var cmd = new ArrayList<String>();
            cmd.add("ffmpeg");
            cmd.add("-hide_banner");
            cmd.add("-loglevel");
            cmd.add("error");
            cmd.add("-nostdin");

            // Read at native rate (real-time) so we don't fast-forward through the video.
            cmd.add("-re");

            if (!source.isTestSrc()) {
                cmd.add("-ss");
                cmd.add(String.format(Locale.ROOT, "%.3f", positionSeconds));
            }

            if (source.isTestSrc()) {
                cmd.add("-f");
                cmd.add("lavfi");
                cmd.add("-i");
                cmd.add("testsrc=size=640x360:rate=" + fps);
            } else {
                cmd.add("-i");
                cmd.add(source.input());
            }

            // Video only; audio (optional) is handled by ffplay.
            cmd.add("-an");

            // Scale to exact target, preserving aspect ratio with letterboxing.
            cmd.add("-vf");
            cmd.add("scale=" + w + ":" + h + ":force_original_aspect_ratio=decrease"
                + ",pad=" + w + ":" + h + ":(ow-iw)/2:(oh-ih)/2:black"
                + ",fps=" + fps);

            cmd.add("-f");
            cmd.add("rawvideo");
            cmd.add("-pix_fmt");
            cmd.add("bgra");
            cmd.add("pipe:1");

            return cmd;
        }

        private void startFrameReaderThread(InputStream stdout, int width, int height) {
            var in = new BufferedInputStream(stdout, 1024 * 64);
            int frameBytes = width * height * 4;
            var buf = new byte[frameBytes];

            var reader = new Thread(() -> {
                try {
                    while (playing && !restartRequested) {
                        if (!readFully(in, buf, frameBytes)) {
                            break;
                        }
                        decodedFrames++;

                        int[] pixels = new int[width * height];
                        // bgra -> argb int
                        int pi = 0;
                        for (int i = 0; i < frameBytes; i += 4) {
                            int b = buf[i] & 0xFF;
                            int g = buf[i + 1] & 0xFF;
                            int r = buf[i + 2] & 0xFF;
                            int a = buf[i + 3] & 0xFF;
                            pixels[pi++] = (a << 24) | (r << 16) | (g << 8) | b;
                        }

                        var img = ImageData.fromArgbPixels(width, height, pixels);
                        long seq = decodedFrames;
                        latest.set(new DecodedFrame(seq, img));
                        // Advance playhead based on our output fps (not the source's nominal fps).
                        positionSeconds += 1.0 / fps;
                    }
                } catch (IOException e) {
                    errorMessage = "ffmpeg decode error: " + e.getMessage();
                }
            }, "video-demo-ffmpeg-frames");
            reader.setDaemon(true);
            reader.start();
        }

        private void startStderrDrainerThread(InputStream stderr) {
            var in = new BufferedInputStream(stderr, 4096);
            var t = new Thread(() -> {
                try {
                    // Keep stderr drained to avoid ffmpeg blocking. Capture a small tail for UI.
                    var baos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    while (playing && !restartRequested) {
                        int r = in.read(buf);
                        if (r < 0) {
                            break;
                        }
                        if (baos.size() < 32 * 1024) {
                            baos.write(buf, 0, r);
                        }
                    }
                    var msg = baos.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (!msg.isEmpty()) {
                        errorMessage = msg;
                    }
                } catch (IOException ignored) {
                    // ignore
                }
            }, "video-demo-ffmpeg-stderr");
            t.setDaemon(true);
            t.start();
        }

        private static boolean readFully(InputStream in, byte[] buf, int len) throws IOException {
            int off = 0;
            while (off < len) {
                int r = in.read(buf, off, len - off);
                if (r < 0) {
                    return false;
                }
                off += r;
            }
            return true;
        }

        private static void sleepQuietly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        private static final class DecodedFrame {
            private final long seq;
            private final ImageData data;

            private DecodedFrame(long seq, ImageData data) {
                this.seq = seq;
                this.data = data;
            }
        }
    }
}


