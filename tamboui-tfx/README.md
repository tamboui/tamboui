# TamboUI TachyonFX

TachyonFX (`dev.tamboui.tfx`) is a library for creating effects and animations for terminal user interfaces. It is a Java port of the Rust library [tachyonfx](https://github.com/junkdog/tachyonfx).

## Features

- **Smooth color transitions** — Fade effects with RGB, HSL, and HSV color space interpolation
- **Text animations** — Dissolve, coalesce, and sweep effects for dynamic text reveals
- **Spatial patterns** — Control effect distribution with radial, diagonal, and sweep patterns
- **Effect composition** — Chain and combine effects for sophisticated animations
- **Cell-precise targeting** — Apply effects to specific regions or cells matching custom criteria
- **20+ easing functions** — Linear, quadratic, cubic, elastic, bounce, and more
- **Java 8+ compatible** — Works with Java 8 and later versions

## Quick Start

Add `tamboui-tfx` to your project dependencies:

```gradle
dependencies {
    implementation(project(":tamboui-tfx"))
    implementation(project(":tamboui-tui"))
    runtimeOnly(project(":tamboui-jline"))
}
```

Create your first effect:

```java
import dev.tamboui.tfx.*;
import dev.tamboui.tfx.pattern.*;
import dev.tamboui.style.Color;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.terminal.Frame;
import java.time.Instant;

public class EffectDemo {
    private Instant lastFrame = Instant.now();
    private EffectManager effectManager = new EffectManager();
    
    public static void main(String[] args) throws Exception {
        new EffectDemo().run();
    }
    
    private void run() throws Exception {
        // Create a fade-in effect
        Effect fadeEffect = Fx.fadeFromFg(Color.CYAN, 2000, Interpolation.SineInOut)
            .withFilter(CellFilter.text());
        
        effectManager.addEffect(fadeEffect);
        
        TuiConfig config = TuiConfig.builder()
            .tickRate(java.time.Duration.ofMillis(16)) // ~60fps
            .build();
        
        try (TuiRunner tui = TuiRunner.create(config)) {
            tui.run(
                (event, runner) -> {
                    if (Keys.isQuit(event)) {
                        runner.quit();
                        return false;
                    }
                    return false;
                },
                frame -> {
                    // Render your widgets first
                    renderContent(frame);
                    
                    // Then apply effects
                    Instant now = Instant.now();
                    long deltaMs = java.time.Duration.between(lastFrame, now).toMillis();
                    lastFrame = now;
                    
                    TFxDuration delta = TFxDuration.fromMillis(deltaMs);
                    effectManager.processEffects(delta, frame.buffer(), frame.area());
                }
            );
        }
    }
    
    private void renderContent(Frame frame) {
        // Your widget rendering code here
    }
}
```

## Basic Concepts

### 1. Effects are Stateful

Create an effect once, then apply it every frame until it completes:

```java
Effect fade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut);
effectManager.addEffect(fade);

// In your render loop
effectManager.processEffects(delta, buffer, area);
```

### 2. Effects Transform Rendered Content

Always render your widgets first, then apply effects:

```java
frame -> {
    // 1. Render widgets
    frame.renderWidget(paragraph, area);
    
    // 2. Apply effects
    effectManager.processEffects(delta, frame.buffer(), frame.area());
}
```

### 3. Effects Compose

Build complex animations by combining simple effects:

```java
// Run effects in sequence
Effect sequence = Fx.sequence(
    Fx.fadeFromFg(Color.BLACK, 500, Interpolation.QuadOut),
    Fx.dissolve(800, Interpolation.Linear)
);

// Or in parallel
Effect parallel = Fx.parallel(
    Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut),
    Fx.sweepIn(Motion.LEFT_TO_RIGHT, 10, 0, Color.BLUE, 2000, Interpolation.QuadOut)
);
```

## Available Effects

### Color Effects

Transform colors over time for smooth transitions.

- **`fadeTo`** / **`fadeFrom`** — Transition between two colors
- **`fadeToFg`** / **`fadeFromFg`** — Foreground color transitions
- **`paint`** / **`paintFg`** / **`paintBg`** — Apply colors directly

```java
// Fade foreground from black to cyan
Effect fade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut);

// Fade from red to blue
Effect colorFade = Fx.fadeFrom(Color.RED, Color.BLUE, 1500, Interpolation.CubicInOut);

// Paint foreground color
Effect paint = Fx.paintFg(Color.YELLOW, 1000, Interpolation.Linear);
```

### Text & Motion Effects

Animate text and cell positions for dynamic content.

- **`dissolve`** / **`dissolveTo`** — Text dissolution effects
- **`coalesce`** / **`coalesceFrom`** — Text materialization effects
- **`sweepIn`** / **`sweepOut`** — Color sweep transitions

```java
// Dissolve text away
Effect dissolve = Fx.dissolve(2000, Interpolation.QuadOut);

// Dissolve to a specific style
Effect dissolveTo = Fx.dissolveTo(
    Style.EMPTY.bg(Color.RED), 
    2000, 
    Interpolation.QuadOut
);

// Sweep in from left to right
Effect sweep = Fx.sweepIn(
    Motion.LEFT_TO_RIGHT, 
    10,      // gradient length
    0,       // randomness
    Color.BLUE, 
    2000, 
    Interpolation.QuadOut
);
```

### Composition Effects

Combine multiple effects for complex animations.

- **`sequence`** — Run effects one after another
- **`parallel`** — Run effects simultaneously

```java
// Chain effects sequentially
Effect chained = Fx.sequence(
    Fx.fadeFromFg(Color.CYAN, 1500, Interpolation.SineInOut),
    Fx.dissolve(2000, Interpolation.QuadOut)
);

// Run effects in parallel
Effect combined = Fx.parallel(
    Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut),
    Fx.dissolve(2500, Interpolation.QuadOut)
);
```

## Spatial Patterns

Control how effects spread across the terminal using patterns.

### Sweep Pattern

Linear progression in cardinal directions:

```java
Effect fadeWithSweep = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
    .withPattern(SweepPattern.leftToRight(15.0f));
```

### Radial Pattern

Expand outward from a center point:

```java
Effect radialFade = Fx.fadeToFg(Color.GREEN, 2000, Interpolation.SineInOut)
    .withPattern(RadialPattern.center().withTransitionWidth(10.0f));

// Custom center point (normalized 0.0-1.0)
Effect customRadial = Fx.dissolve(2000, Interpolation.QuadOut)
    .withPattern(RadialPattern.at(0.5f, 0.3f).withTransitionWidth(8.0f));
```

### Diagonal Pattern

Sweep across diagonally:

```java
Effect diagonalFade = Fx.fadeToFg(Color.MAGENTA, 2000, Interpolation.SineInOut)
    .withPattern(DiagonalPattern.topLeftToBottomRight().withTransitionWidth(15.0f));
```

## Interpolation

Choose from 20+ easing functions to control animation timing:

- **Linear** — Constant speed
- **Quad** / **Cubic** / **Quart** / **Quint** — Polynomial easing (in/out/in-out)
- **Sine** — Smooth sinusoidal curves
- **Circ** — Circular easing
- **Expo** — Exponential acceleration/deceleration
- **Elastic** — Bouncy elastic effect
- **Bounce** — Bouncing effect
- **Back** — Overshoot effect

```java
// Smooth ease-in-out
Effect smooth = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut);

// Bouncy effect
Effect bouncy = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.BounceOut);

// Elastic effect
Effect elastic = Fx.fadeToFg(Color.GREEN, 2000, Interpolation.ElasticOut);
```

## Cell Filtering

Apply effects selectively to specific cells or regions:

```java
// Only apply to text cells
Effect textOnly = Fx.dissolve(2000, Interpolation.Linear)
    .withFilter(CellFilter.text());

// Only apply to cells with specific foreground color
Effect colorFilter = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
    .withFilter(CellFilter.fgColor(Color.RED));

// Apply to outer margin
Effect marginEffect = Fx.sweepIn(Motion.LEFT_TO_RIGHT, 10, 0, Color.BLUE, 2000, Interpolation.QuadOut)
    .withFilter(CellFilter.outer(Margin.uniform(1)));

// Combine filters
CellFilter combined = CellFilter.allOf(
    CellFilter.text(),
    CellFilter.fgColor(Color.WHITE)
);
Effect filtered = Fx.dissolve(2000, Interpolation.Linear)
    .withFilter(combined);
```

## Color Spaces

Control how colors are interpolated:

```java
// RGB interpolation (fastest, but not perceptually uniform)
Effect rgbFade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
    .withColorSpace(TFxColorSpace.RGB);

// HSL interpolation (default - better perceptual quality)
Effect hslFade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
    .withColorSpace(TFxColorSpace.HSL);

// HSV interpolation
Effect hsvFade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
    .withColorSpace(TFxColorSpace.HSV);
```

## Examples

See the `demos/fx-demo` module for a complete interactive demo showcasing all available effects:

```bash
./gradlew :demos:fx-demo:run
```

The demo includes:

- Fade effects with different color spaces
- Dissolve and coalesce effects
- Sweep effects in all directions
- Paint effects
- Pattern combinations
- Effect composition (sequence and parallel)
- Various interpolation types

## Requirements

- Java 8 or later
- TamboUI core module (`tamboui-core`)
- TamboUI TUI module (`tamboui-tui`) for the demo application

## Port Status

This library is a Java port of the Rust [tachyonfx](https://github.com/junkdog/tachyonfx) library. The port maintains the core concepts and API structure while adapting to Java idioms and TamboUI's architecture.

### Ported Features

✅ **Core System**

- Effect management (`EffectManager`)
- Effect timer and interpolation (`EffectTimer`, `Interpolation`)
- Shader interface and effect composition
- Cell filtering and iteration

✅ **Effects**

- Fade effects (`fadeTo`, `fadeFrom`, `fadeToFg`, `fadeFromFg`)
- Dissolve effects (`dissolve`, `dissolveTo`)
- Coalesce effects (`coalesce`, `coalesceFrom`)
- Sweep effects (`sweepIn`, `sweepOut`)
- Paint effects (`paint`, `paintFg`, `paintBg`)
- Effect composition (`sequence`, `parallel`)

✅ **Patterns**

- Sweep pattern (linear directional)
- Radial pattern (center-out expansion)
- Diagonal pattern (diagonal sweeps)
- Identity pattern (no spatial transformation)

✅ **Interpolation**

- All 20+ easing functions (Linear, Quad, Cubic, Quart, Quint, Sine, Circ, Expo, Elastic, Bounce, Back)

✅ **Color Spaces**

- RGB interpolation
- HSL interpolation
- HSV interpolation

### Not Yet Ported

The following features from the original Rust library are not yet implemented:

❌ **Advanced Effects**
- `hsl_shift` / `hsl_shift_fg` — HSL color space animations
- `evolve` / `evolve_into` / `evolve_from` — Character evolution effects
- `slide_in` / `slide_out` — Directional sliding animations
- `explode` — Particle dispersion effect
- `expand` — Bidirectional expansion
- `stretch` — Unidirectional stretching
- `glitch` — Glitch effects
- `term256_colors` — 256-color mode downsampling

❌ **Control Effects**
- `repeat` / `repeating` — Loop effects
- `ping_pong` — Forward/reverse playback
- `delay` / `sleep` — Pause effects
- `prolong_start` / `prolong_end` — Extend duration
- `freeze_at` — Freeze at specific point
- `remap_alpha` — Remap progress range
- `run_once` — Ensure single execution
- `never_complete` / `timed_never_complete` — Indefinite effects
- `consume_tick` — Single-frame delay
- `with_duration` — Override duration

❌ **Patterns**

- `CheckerboardPattern` — Grid-based alternating pattern
- `CoalescePattern` / `DissolvePattern` — Organic randomized patterns

❌ **Geometry Effects**

- `translate` — Move content by offset
- `resize_area` — Scale effect bounds
- `translate_buf` — Copy and move buffer content

❌ **Advanced Features**

- Effect DSL (runtime effect compilation from strings)
- Custom effect functions (`effect_fn`, `effect_fn_buf`)
- Offscreen buffer support
- WebAssembly support

### Java-Specific Adaptations

- **Class Naming**: Classes that shadow JDK types are prefixed with `TFx`:
  - `Duration` → `TFxDuration` (avoids conflict with `java.time.Duration`)
  - `Math` → `TFxMath` (avoids conflict with `java.lang.Math`)
  - `ColorSpace` → `TFxColorSpace` (avoids conflict with `java.awt.color.ColorSpace`)

- **Package Structure**: Uses `dev.tamboui.tfx` package namespace with `effects` subpackage for shader implementations

- **Java 8 Compatibility**: Avoids Java 9+ features (sealed classes, records, switch expressions) to maintain Java 8 compatibility

### Contributing

Contributions are welcome! If you'd like to port additional features from the original Rust library, please check existing issues or create a new one to discuss the implementation approach.

## License

MIT License - see [LICENSE](../LICENSE) for details.
