///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.shapes.Points;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * New Year countdown demo inspired by https://github.com/willmcgugan/ny2026, adapted to TamboUI.
 *
 * - Big digital countdown clock to local New Year (Jan 1st 00:00 local time)
 * - Firework rockets + particle explosions (triggered at midnight or Space)
 * - 3D perspective projection with camera movement
 * - Procedural stereo audio system
 * - 60 FPS rendering
 * - Braille dot rendering for clock digits
 */
public final class NewYearDemo extends ToolkitApp {

    private static final Duration TICK = Duration.ofMillis(16); // ~60 fps
    private static final Duration MANUAL_SHOW_DURATION = Duration.ofSeconds(6);
    private static final Duration MIDNIGHT_SHOW_DURATION = Duration.ofSeconds(18);

    private final ZoneId zone = ZoneId.systemDefault();
    private final Random random = new Random();
    private final FireworksShow fireworks = new FireworksShow(random);
    private final SoundManager soundManager = new SoundManager();

    private ZonedDateTime target;
    private boolean midnightTriggered = false;
    private boolean simulating = false;

    private long lastUpdateNanos = 0L;
    private double cameraZ = 0.0;
    private static final double CAMERA_SPEED = 15.0; // pixels/second through Z-space

    private NewYearDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new NewYearDemo().run();
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .tickRate(TICK)
            .mouseCapture(false)
            .build();
    }

    @Override
    protected void onStart() {
        // Ensure we receive key events immediately.
        if (runner() != null) {
            runner().focusManager().setFocus("root");
        }
        target = computeNextNewYear(zone);
        lastUpdateNanos = System.nanoTime();
    }

    @Override
    protected Element render() {
        Rect area = Rect.of(80, 24);
        if (runner() != null) {
            area = runner().tuiRunner().terminal().area();
        }
        int innerW = Math.max(1, area.width() - 2);
        int innerH = Math.max(1, area.height() - 2);

        ZonedDateTime now = ZonedDateTime.now(zone);
        Duration remaining = Duration.between(now, target);
        // Auto-trigger at (or after) midnight, and clamp the display at 00:00:00.
        if (remaining.isZero() || remaining.isNegative()) {
            if (!midnightTriggered) {
                midnightTriggered = true;
                fireworks.startShow(MIDNIGHT_SHOW_DURATION);
            }
            remaining = Duration.ZERO;
        }
        // Keep target updated to current time when simulating to maintain 00:00:00
        if (simulating) {
           // target = now;
            remaining = Duration.ZERO;
        }

        // Update simulation time step.
        long nowNanos = System.nanoTime();
        double dt = Math.max(0.0, Math.min(0.2, (nowNanos - lastUpdateNanos) / 1_000_000_000.0));
        lastUpdateNanos = nowNanos;
        
        // Animate camera moving forward through Z-space
        cameraZ += CAMERA_SPEED * dt;
        
        fireworks.update(dt, innerW, innerH, cameraZ, soundManager, null);

        String bottomHelp = " [Space] Fireworks   [s] Simulate   [q] Quit ";
        final ZonedDateTime nowFinal = now;
        final Duration remainingFinal = remaining;
        final int innerWFinal = innerW;
        final int innerHFinal = innerH;

        return panel(
            canvas(0, innerWFinal, 0, innerHFinal)
                .marker(Marker.BRAILLE)
                .paint(ctx -> {
                    // Background fireworks (points/lines) with 3D perspective
                    fireworks.render(ctx, cameraZ, innerWFinal, innerHFinal);

                    // Foreground clock + labels (rendered above points)
                    renderOverlay(ctx, innerWFinal, innerHFinal, nowFinal, remainingFinal);
                })
                .fill()
        )
            .id("root")
            .focusable()
            .rounded()
            .borderColor(Color.DARK_GRAY)
            .focusedBorderColor(Color.CYAN)
            .bottomTitle(bottomHelp)
            .onKeyEvent(e -> {
                if (e != null) {
                    if (e.isChar(' ')) {
                        // Launch a single firework on space press
                        fireworks.launchSingleFirework(innerWFinal, innerHFinal, cameraZ, soundManager);
                        return EventResult.HANDLED;
                    } else if (e.isChar('s') || e.isChar('S')) {
                        // Simulate New Year: trigger midnight show and set countdown to zero
                        simulating = true;
                        if (!midnightTriggered) {
                            midnightTriggered = true;
                            fireworks.startShow(MIDNIGHT_SHOW_DURATION);
                           
                        }
                        return EventResult.HANDLED;
                    }
                }
                return EventResult.UNHANDLED;
            });
    }

    private void renderOverlay(Context ctx, int w, int h, ZonedDateTime now, Duration remaining) {
        int nextYear = target.getYear();
        long totalSeconds = Math.max(0, remaining.toSeconds());
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        String clock = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        SevenSegFont font = SevenSegFont.DEFAULT;
        List<String> lines = font.render(clock);

        // Centered big clock.
        int clockHeight = lines.size();
        int clockWidth = 0;
        for (String line : lines) {
            clockWidth = Math.max(clockWidth, line.length());
        }

        double startX = Math.max(0, (w - clockWidth) / 2.0);
        double topY = Math.max(0, h * 0.72);

        Color clockColor = fireworks.isShowActive() ? Color.YELLOW : Color.CYAN;
        
        // Render clock as braille dots (points) instead of text
        // Pack points closely together to form solid-looking braille patterns
        List<double[]> clockPoints = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // Canvas y is bottom-origin; decreasing y moves down visually.
            double baseY = topY - i;
            for (int j = 0; j < line.length(); j++) {
                char ch = line.charAt(j);
                if (ch != ' ' && ch != '░') {
                    // Render points in a 2x3 grid to create solid braille patterns
                    // This creates dense, close dots that form the digit shape
                    double baseX = startX + j;
                    for (int dy = 0; dy < 3; dy++) {
                        for (int dx = 0; dx < 2; dx++) {
                            // Small offsets to pack into braille cells
                            double x = baseX + dx * 0.4;
                            double y = baseY + dy * 0.3;
                            clockPoints.add(new double[] {x, y});
                        }
                    }
                }
            }
        }
        
        if (!clockPoints.isEmpty()) {
            double[][] coords = new double[clockPoints.size()][2];
            for (int i = 0; i < clockPoints.size(); i++) {
                coords[i] = clockPoints.get(i);
            }
            ctx.draw(Points.of(coords, clockColor));
        }

        // Days label (normal size)
        if (days > 0) {
            String daysLine = String.format("%d day%s", days, days == 1 ? "" : "s");
            int dx = Math.max(0, (w - daysLine.length()) / 2);
            ctx.print(dx, topY + 2, Line.from(Span.raw(daysLine).magenta().bold()));
        }

        // Title / subtext
        String title = "Counting down to " + nextYear + " (local time)";
        int titleX = Math.max(0, (w - title.length()) / 2);
        ctx.print(titleX, topY + 4, Line.from(Span.raw(title).gray()));

        // At/after midnight: happy message
        if (midnightTriggered || remaining.isZero()) {
            String msg = "HAPPY NEW YEAR " + nextYear + "!";
            int mx = Math.max(0, (w - msg.length()) / 2);
            double my = Math.max(0, Math.round(h * 0.25));
            ctx.print(mx, my, Line.from(Span.raw(msg).green().bold()));
        }

        // Corner info
        String nowLine = now.toLocalTime().withNano(0).toString();
        ctx.print(1, h - 2, Line.from(Span.raw("Now: " + nowLine).dim().gray()));
    }

    private static ZonedDateTime computeNextNewYear(ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        int nextYear = now.getYear() + 1;
        return ZonedDateTime.of(LocalDate.of(nextYear, 1, 1), LocalTime.MIDNIGHT, zone);
    }

    @Override
    protected void onStop() {
        soundManager.stop();
    }

    /**
     * Manages sound generation and playback for fireworks explosions.
     * Generates procedural explosion sounds with stereo panning.
     */
    static final class SoundManager {
        private static final int SAMPLE_RATE = 22050;
        private static final double DURATION = 1.5; // seconds
        private static final int MAX_CONCURRENT_SOUNDS = 8;
        private static final int PAN_POSITIONS = 17; // -1.0 to 1.0 in steps of 0.125

        private final ConcurrentLinkedQueue<SoundClip> activeSounds = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final byte[][] stereoCache = new byte[PAN_POSITIONS][];
        private final byte[] monoExplosionSound;
        private Thread audioThread;
        private SourceDataLine audioLine;

        SoundManager() {
            // Generate mono explosion sound
            monoExplosionSound = generateExplosionSound();
            
            // Pre-generate stereo variations at different pan positions
            generateStereoCache();
            
            // Start audio thread
            startAudioThread();
        }

        private byte[] generateExplosionSound() {
            int samples = (int) (SAMPLE_RATE * DURATION);
            float[] wave = new float[samples];
            Random rng = new Random();
            
            // Generate time array
            for (int i = 0; i < samples; i++) {
                double t = (double) i / SAMPLE_RATE;
                
                // White noise
                double noise = (rng.nextGaussian() * 0.5);
                
                // Low-frequency rumble
                double rumbleFreq = 60.0;
                double rumble = Math.sin(2 * Math.PI * rumbleFreq * t);
                rumble += 0.5 * Math.sin(2 * Math.PI * rumbleFreq * 2 * t);
                
                // Combine noise and rumble
                wave[i] = (float) (0.7 * noise + 0.3 * rumble);
                
                // Apply exponential decay envelope
                double envelope = Math.exp(-3 * t / DURATION);
                wave[i] *= (float) envelope;
                
                // Add crackle (short bursts)
                if (rng.nextDouble() < 0.05) {
                    wave[i] += (float) (rng.nextGaussian() * 0.5 * envelope);
                }
            }
            
            // Apply simple low-pass filter (moving average)
            int windowSize = 15;
            float[] filtered = new float[samples];
            for (int i = 0; i < samples; i++) {
                double sum = 0;
                int count = 0;
                for (int j = Math.max(0, i - windowSize / 2); j < Math.min(samples, i + windowSize / 2 + 1); j++) {
                    sum += wave[j];
                    count++;
                }
                filtered[i] = (float) (sum / count);
            }
            
            // Normalize and convert to bytes (16-bit PCM)
            byte[] result = new byte[samples * 2];
            float max = 0;
            for (float f : filtered) {
                max = Math.max(max, Math.abs(f));
            }
            if (max > 0) {
                float scale = 0.5f / max; // Reduce volume to 50%
                for (int i = 0; i < samples; i++) {
                    short sample = (short) (filtered[i] * scale * Short.MAX_VALUE);
                    result[i * 2] = (byte) (sample & 0xFF);
                    result[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }
            }
            
            return result;
        }

        private void generateStereoCache() {
            if (monoExplosionSound == null) {
                return;
            }
            
            int samples = monoExplosionSound.length / 2; // 16-bit samples
            
            for (int i = 0; i < PAN_POSITIONS; i++) {
                double pan = -1.0 + (i * 0.125); // -1.0 to 1.0
                
                // Constant power panning
                double panAngle = (pan + 1) * (Math.PI / 4); // Map -1..1 to 0..pi/2
                double leftGain = Math.cos(panAngle);
                double rightGain = Math.sin(panAngle);
                
                // Create stereo audio
                byte[] stereo = new byte[samples * 4]; // 2 channels * 2 bytes per sample
                for (int j = 0; j < samples; j++) {
                    int monoIdx = j * 2;
                    short monoSample = (short) ((monoExplosionSound[monoIdx] & 0xFF) | 
                                                ((monoExplosionSound[monoIdx + 1] & 0xFF) << 8));
                    
                    short leftSample = (short) (monoSample * leftGain);
                    short rightSample = (short) (monoSample * rightGain);
                    
                    int stereoIdx = j * 4;
                    stereo[stereoIdx] = (byte) (leftSample & 0xFF);
                    stereo[stereoIdx + 1] = (byte) ((leftSample >> 8) & 0xFF);
                    stereo[stereoIdx + 2] = (byte) (rightSample & 0xFF);
                    stereo[stereoIdx + 3] = (byte) ((rightSample >> 8) & 0xFF);
                }
                
                stereoCache[i] = stereo;
            }
        }

        private void startAudioThread() {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                
                if (!AudioSystem.isLineSupported(info)) {
                    return; // Audio not supported, silently fail
                }
                
                audioLine = (SourceDataLine) AudioSystem.getLine(info);
                audioLine.open(format);
                audioLine.start();
                
                running.set(true);
                audioThread = new Thread(this::audioPlaybackLoop, "FireworksAudio");
                audioThread.setDaemon(true);
                audioThread.start();
            } catch (LineUnavailableException e) {
                // Silently fail if audio is unavailable
            }
        }

        private void audioPlaybackLoop() {
            byte[] buffer = new byte[4096];
            
            while (running.get() && audioLine != null && audioLine.isOpen()) {
                try {
                    // Mix all active sounds
                    int bytesToWrite = Math.min(buffer.length, audioLine.available());
                    if (bytesToWrite > 0) {
                        // Zero buffer (16-bit stereo: 4 bytes per sample = 2 channels * 2 bytes)
                        for (int i = 0; i < bytesToWrite; i++) {
                            buffer[i] = 0;
                        }
                        
                        // Mix sounds (16-bit stereo: L, R, L, R...)
                        Iterator<SoundClip> it = activeSounds.iterator();
                        while (it.hasNext()) {
                            SoundClip clip = it.next();
                            
                            int remaining = clip.data.length - clip.position;
                            int toMix = Math.min(remaining, bytesToWrite);
                            
                            // Mix 16-bit stereo samples (4 bytes per sample)
                            for (int i = 0; i < toMix; i += 4) {
                                if (clip.position + i + 3 >= clip.data.length) {
                                    break;
                                }
                                
                                // Left channel
                                short clipLeft = (short) ((clip.data[clip.position + i] & 0xFF) | 
                                                           ((clip.data[clip.position + i + 1] & 0xFF) << 8));
                                short bufferLeft = (short) ((buffer[i] & 0xFF) | 
                                                             ((buffer[i + 1] & 0xFF) << 8));
                                int mixedLeft = bufferLeft + clipLeft;
                                mixedLeft = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedLeft));
                                buffer[i] = (byte) (mixedLeft & 0xFF);
                                buffer[i + 1] = (byte) ((mixedLeft >> 8) & 0xFF);
                                
                                // Right channel
                                short clipRight = (short) ((clip.data[clip.position + i + 2] & 0xFF) | 
                                                            ((clip.data[clip.position + i + 3] & 0xFF) << 8));
                                short bufferRight = (short) ((buffer[i + 2] & 0xFF) | 
                                                              ((buffer[i + 3] & 0xFF) << 8));
                                int mixedRight = bufferRight + clipRight;
                                mixedRight = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedRight));
                                buffer[i + 2] = (byte) (mixedRight & 0xFF);
                                buffer[i + 3] = (byte) ((mixedRight >> 8) & 0xFF);
                            }
                            
                            clip.position += toMix;
                            
                            // Remove when finished
                            if (clip.position >= clip.data.length) {
                                it.remove();
                            }
                        }
                        
                        // Write to audio line
                        audioLine.write(buffer, 0, bytesToWrite);
                    } else {
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Silently handle errors
                    break;
                }
            }
        }

        void playExplosion(double x, double screenWidth) {
            if (monoExplosionSound == null || stereoCache[0] == null) {
                return;
            }
            
            if (activeSounds.size() >= MAX_CONCURRENT_SOUNDS) {
                return; // Too many sounds playing
            }
            
            // Calculate pan position: -1 (left) to 1 (right)
            double pan = (x / screenWidth) * 2.0 - 1.0;
            pan = Math.max(-1.0, Math.min(1.0, pan));
            
            // Quantize to nearest cached position
            int panIndex = (int) Math.round((pan + 1.0) / 0.125);
            panIndex = Math.max(0, Math.min(PAN_POSITIONS - 1, panIndex));
            
            byte[] stereoData = stereoCache[panIndex];
            if (stereoData != null) {
                activeSounds.offer(new SoundClip(stereoData));
            }
        }


        void stop() {
            running.set(false);
            if (audioThread != null) {
                audioThread.interrupt();
                try {
                    audioThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (audioLine != null && audioLine.isOpen()) {
                audioLine.stop();
                audioLine.close();
            }
        }

        static final class SoundClip {
            final byte[] data;
            int position;

            SoundClip(byte[] data) {
                this.data = data;
                this.position = 0;
            }
        }
    }

    /**
     * Simple fireworks show: rockets fly up, then explode into particles which fall and fade.
     * Coordinates are in 3D space with perspective projection.
     */
    static final class FireworksShow {
        private static final double GRAVITY = -100.0; // units / s^2 (negative = downward, since y increases upward)
        private static final double DRAG = 0.97; // Air resistance (matching Python)

        private final Random rng;
        private final List<Rocket> rockets = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();

        private double showTimeLeft = 0.0;
        private double rocketSpawnCooldown = 0.0;

        FireworksShow(Random rng) {
            this.rng = Objects.requireNonNull(rng, "rng");
        }

        boolean isShowActive() {
            return showTimeLeft > 0.0;
        }

        void startShow(Duration duration) {
            showTimeLeft = Math.max(showTimeLeft, duration.toMillis() / 1000.0);
            // Spawn a couple immediately for responsiveness.
            rocketSpawnCooldown = 0.0;
        }

        void launchSingleFirework(int width, int height, double cameraZ, SoundManager soundManager) {
            // Launch just one firework immediately
            spawnRocket(width, height, cameraZ, soundManager);
        }

        void update(double dt, int width, int height, double cameraZ, SoundManager soundManager, Runnable onExplosion) {
            if (dt <= 0) {
                return;
            }

            // Spawn rockets while show is active.
            if (showTimeLeft > 0.0) {
                showTimeLeft = Math.max(0.0, showTimeLeft - dt);
                rocketSpawnCooldown -= dt;
                if (rocketSpawnCooldown <= 0.0) {
                    spawnRocket(width, height, cameraZ, soundManager);
                    // Random cadence (denser near the start).
                    rocketSpawnCooldown = 0.2 + rng.nextDouble() * 0.6;
                }
            }

            // Update rockets (and explode if needed).
            for (Iterator<Rocket> it = rockets.iterator(); it.hasNext(); ) {
                Rocket r = it.next();
                r.update(dt, width, height, GRAVITY);

                // Store trail position for launch phase
                r.trail.add(new double[] {r.x, r.y, r.z});
                if (r.trail.size() > 15) {
                    r.trail.remove(0);
                }

                if (r.shouldExplode()) {
                    explode(r, width, soundManager, onExplosion);
                    it.remove();
                } else if (r.isOutOfBounds(width, height, cameraZ)) {
                    it.remove();
                }
            }

            // Update particles with lighter gravity for explosion (matching Python)
            double particleGravity = -50.0; // Lighter than rocket gravity
            for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
                Particle p = it.next();
                p.update(dt, particleGravity, DRAG);
                // Remove if dead or fallen below bottom
                if (p.life <= 0.0 || p.y < 0) {
                    it.remove();
                }
            }
        }

        void render(Context ctx, double cameraZ, int width, int height) {
            double centerX = width / 2.0;
            double centerY = height / 2.0;
            double cameraDistance = 200.0;

            // Draw rocket trails with perspective as continuous lines
            for (Rocket r : rockets) {
                if (r.trail.size() > 1) {
                    // Convert trail points to screen coordinates
                    List<double[]> screenPoints = new ArrayList<>();
                    for (double[] point : r.trail) {
                        double zRel = point[2] - cameraZ;
                        double zOffset = zRel + cameraDistance;
                        if (zOffset > 0) {
                            double scale = cameraDistance / zOffset;
                            double sx = centerX + (point[0] - centerX) * scale;
                            double sy = centerY + (point[1] - centerY) * scale;
                            if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                                screenPoints.add(new double[] {sx, sy});
                            }
                        }
                    }
                    // Draw continuous line segments for the trail
                    if (screenPoints.size() > 1) {
                        for (int i = 0; i < screenPoints.size() - 1; i++) {
                            double[] p1 = screenPoints.get(i);
                            double[] p2 = screenPoints.get(i + 1);
                            ctx.draw(dev.tamboui.widgets.canvas.shapes.Line.of(p1[0], p1[1], p2[0], p2[1], r.fireworkColor));
                        }
                    }
                }
            }

            // Group particles by color for efficient Points rendering with perspective.
            Map<Color, List<double[]>> buckets = new HashMap<>();
            for (Particle p : particles) {
                double zRel = p.z - cameraZ;
                double zOffset = zRel + cameraDistance;
                if (zOffset <= 0) {
                    continue; // Behind camera
                }
                double scale = cameraDistance / zOffset;
                double sx = centerX + (p.x - centerX) * scale;
                double sy = centerY + (p.y - centerY) * scale;
                if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                    Color color = p.color;
                    List<double[]> list = buckets.computeIfAbsent(color, k -> new ArrayList<>());
                    list.add(new double[] {sx, sy});
                }
            }
            for (Map.Entry<Color, List<double[]>> e : buckets.entrySet()) {
                List<double[]> pts = e.getValue();
                if (pts == null || pts.isEmpty()) {
                    continue;
                }
                double[][] coords = new double[pts.size()][2];
                for (int i = 0; i < pts.size(); i++) {
                    coords[i] = pts.get(i);
                }
                ctx.draw(Points.of(coords, e.getKey()));
            }
        }

        private void spawnRocket(int width, int height, double cameraZ) {
            spawnRocket(width, height, cameraZ, null);
        }

        private void spawnRocket(int width, int height, double cameraZ, SoundManager soundManager) {
            if (width <= 0 || height <= 0) {
                return;
            }
            // Canvas: y=0 is bottom, y increases upward
            // Start at very bottom (y=0 or slightly above)
            double x = width * 0.2 + rng.nextDouble() * (width * 0.6);
            double y = 0.0; // Start at absolute bottom
            double z = cameraZ + 50.0 + rng.nextDouble() * 250.0; // Spawn ahead of camera
            
            // Calculate target explosion height - should peak well above clock level
            // Clock is at ~72% from bottom, so target 80-98% from bottom (well above clock)
            double targetY = height * 0.80 + rng.nextDouble() * (height * 0.18);
            
            // Calculate required launch velocity to reach target height
            // Using kinematic equation: v² = u² + 2as
            // At apex: v = 0, so u² = -2as
            // distanceToTarget is positive (going up)
            double distanceToTarget = targetY - y; // Positive (going up) - larger range now
            // GRAVITY is negative, so -2 * GRAVITY is positive
            // Keep original velocity calculation (no reduction) to reach the higher peak
            double requiredVelocity = Math.sqrt(-2 * GRAVITY * distanceToTarget);
            
            double vx = (rng.nextDouble() - 0.5) * 40.0;
            double vy = requiredVelocity; // Positive because upward (y increases upward)
            double vz = 0.0;
            double fuse = 0.9 + rng.nextDouble() * 0.8;
            
            // Generate random firework color matching Python palette
            Color fireworkColor = randomFireworkColor(rng);
            rockets.add(new Rocket(x, y, z, vx, vy, vz, fuse, fireworkColor));
        }
        
        private Color randomFireworkColor(Random rng) {
            // Python firework colors - realistic pyrotechnic colors
            int[][] colors = {
                {255, 50, 50},      // Red (Strontium)
                {255, 140, 0},       // Orange (Calcium)
                {255, 215, 0},       // Gold/Yellow (Sodium, Iron)
                {240, 240, 240},     // White/Silver (Aluminum, Magnesium)
                {50, 255, 50},       // Green (Barium)
                {100, 150, 255},     // Blue (Copper)
                {200, 100, 255},     // Purple (Strontium + Copper)
                {255, 192, 203},     // Pink (Strontium + Titanium)
                {0, 255, 255},       // Cyan/Turquoise (Copper compounds)
                {220, 20, 60},       // Deep Red/Crimson (Lithium)
                {200, 255, 0},       // Lime Green (Barium with additives)
                {80, 200, 255},      // Electric Blue (Copper chloride)
                {180, 140, 255},     // Lavender (Potassium/Rubidium)
                {255, 180, 120},     // Peach (Calcium + Strontium)
                {255, 191, 0},       // Amber (Iron + Charcoal)
                {255, 250, 200},     // Golden White (Titanium sparkles)
                {255, 0, 255},       // Magenta (Strontium + Copper)
                {150, 255, 200},     // Mint Green (Barium + Copper)
            };
            int[] rgb = colors[rng.nextInt(colors.length)];
            return Color.rgb(rgb[0], rgb[1], rgb[2]);
        }

        private void explode(Rocket r, int width, SoundManager soundManager, Runnable onExplosion) {
            // Play explosion sound with stereo positioning
            if (soundManager != null) {
                soundManager.playExplosion(r.x, width);
            }
            
            // Trigger camera pulse on explosion
            if (onExplosion != null) {
                onExplosion.run();
            }

            Color baseColor = r.fireworkColor;
            // Increased particle count: 450-750 for denser explosion
            int count = 450 + rng.nextInt(301);
            // Smaller, more contained explosion - half the speed for tighter circle
            double speed = 50.0 + rng.nextDouble() * 25.0; // 50-75 (half of previous)
            
            for (int i = 0; i < count; i++) {
                // Generate particles in a 2D circle in X-Y plane (perpendicular to camera)
                // This ensures circular appearance when viewed from front
                double angle = rng.nextDouble() * Math.PI * 2.0; // Random angle in circle
                double radius = rng.nextDouble(); // Random distance from center (0 to 1)
                // Use sqrt for uniform distribution in circle area
                radius = Math.sqrt(radius);
                
                // Convert to Cartesian coordinates in X-Y plane
                double vx = speed * radius * Math.cos(angle);
                double vy = speed * radius * Math.sin(angle);
                // Small random Z component for slight depth variation
                double vz = (rng.nextDouble() - 0.5) * speed * 0.3;
                
                // Random lifetime: 1.8-2.5 seconds (matching Python)
                double life = 1.8 + rng.nextDouble() * 0.7;
                // 20% white sparkles, 80% base color
                Color color = rng.nextDouble() < 0.20 ? Color.WHITE : baseColor;
                particles.add(new Particle(r.x, r.y, r.z, vx, vy, vz, life, color));
            }
        }
    }

    static final class Rocket {
        double x;
        double y;
        double z;
        double vx;
        double vy;
        double vz;
        double fuse;
        double age;
        boolean apexReached = false;
        double timeSinceApex = 0.0;
        final List<double[]> trail = new ArrayList<>(); // [x, y, z] points
        final Color fireworkColor; // Color for this firework (used for trail and explosion)

        Rocket(double x, double y, double z, double vx, double vy, double vz, double fuse, Color fireworkColor) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.fuse = fuse;
            this.age = 0.0;
            this.fireworkColor = fireworkColor;
        }

        void update(double dt, int width, int height, double gravity) {
            age += dt;

            // Integrate
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;
            vy += gravity * dt; // gravity is negative, so this decreases vy

            // Check if apex reached (velocity becomes negative/downward)
            // In this coordinate system, vy positive = upward, vy negative = downward
            if (vy < 0 && !apexReached) {
                apexReached = true;
            }

            // If apex reached, wait about 1 second before exploding
            if (apexReached) {
                timeSinceApex += dt;
            }
        }

        boolean shouldExplode() {
            // Explode after fuse or 1.5 seconds after apex (longer peak/fall time)
            return age >= fuse || (apexReached && timeSinceApex >= 1.5);
        }

        boolean isOutOfBounds(int width, int height, double cameraZ) {
            // Remove if behind camera or too old (fallen below bottom)
            return (z - cameraZ < -50.0) || (y < 0 && age > 1.0);
        }
    }

    static final class Particle {
        double x;
        double y;
        double z;
        double vx;
        double vy;
        double vz;
        double life;
        final Color color;

        Particle(double x, double y, double z, double vx, double vy, double vz, double life, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.life = life;
            this.color = color != null ? color : Color.WHITE;
        }

        void update(double dt, double gravity, double drag) {
            life -= dt;
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;
            // Apply air resistance (per-frame damping)
            vx *= drag;
            vy *= drag;
            vz *= drag;
            // Apply gravity (negative = downward in this coordinate system)
            vy += gravity * dt;
        }
    }

    /**
     * Tiny 7-seg-ish font for "HH:MM:SS" (height 7).
     */
    static final class SevenSegFont {
        static final SevenSegFont DEFAULT = new SevenSegFont();

        private final Map<Character, String[]> glyphs = Map.ofEntries(
            Map.entry('0', new String[] {
                "█████",
                "█   █",
                "█   █",
                "█   █",
                "█   █",
                "█   █",
                "█████"
            }),
            Map.entry('1', new String[] {
                "    █",
                "    █",
                "    █",
                "    █",
                "    █",
                "    █",
                "    █"
            }),
            Map.entry('2', new String[] {
                "█████",
                "    █",
                "    █",
                "█████",
                "█    ",
                "█    ",
                "█████"
            }),
            Map.entry('3', new String[] {
                "█████",
                "    █",
                "    █",
                "█████",
                "    █",
                "    █",
                "█████"
            }),
            Map.entry('4', new String[] {
                "█   █",
                "█   █",
                "█   █",
                "█████",
                "    █",
                "    █",
                "    █"
            }),
            Map.entry('5', new String[] {
                "█████",
                "█    ",
                "█    ",
                "█████",
                "    █",
                "    █",
                "█████"
            }),
            Map.entry('6', new String[] {
                "█████",
                "█    ",
                "█    ",
                "█████",
                "█   █",
                "█   █",
                "█████"
            }),
            Map.entry('7', new String[] {
                "█████",
                "    █",
                "    █",
                "    █",
                "    █",
                "    █",
                "    █"
            }),
            Map.entry('8', new String[] {
                "█████",
                "█   █",
                "█   █",
                "█████",
                "█   █",
                "█   █",
                "█████"
            }),
            Map.entry('9', new String[] {
                "█████",
                "█   █",
                "█   █",
                "█████",
                "    █",
                "    █",
                "█████"
            }),
            Map.entry(':', new String[] {
                "     ",
                "  █  ",
                "  █  ",
                "     ",
                "  █  ",
                "  █  ",
                "     "
            })
        );

        List<String> render(String text) {
            String s = text != null ? text : "";
            int height = 7;
            String[] out = new String[height];
            for (int i = 0; i < height; i++) {
                out[i] = "";
            }

            for (int idx = 0; idx < s.length(); idx++) {
                char ch = s.charAt(idx);
                String[] g = glyphs.getOrDefault(ch, blank(height));
                for (int row = 0; row < height; row++) {
                    out[row] += g[row];
                    if (idx < s.length() - 1) {
                        out[row] += " ";
                    }
                }
            }

            List<String> lines = new ArrayList<>(height);
            for (int i = 0; i < height; i++) {
                lines.add(rtrim(out[i]));
            }
            return lines;
        }

        private static String[] blank(int h) {
            String[] b = new String[h];
            for (int i = 0; i < h; i++) {
                b[i] = "     ";
            }
            return b;
        }

        private static String rtrim(String s) {
            int i = s.length() - 1;
            while (i >= 0 && s.charAt(i) == ' ') {
                i--;
            }
            return s.substring(0, i + 1);
        }
    }
}

