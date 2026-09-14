# TamboUI Ratty Graphics Protocol Module

**Ratty Graphics Protocol (RGP)** support for rendering 3D objects in terminals.

## Overview

This module implements the [Ratty Graphics Protocol](https://github.com/orhun/ratty/blob/main/protocols/graphics.md), a terminal protocol for displaying 3D objects (OBJ/GLB files) as inline terminal elements.

RGP allows terminals to:
- Display 3D objects in terminal cells
- Animate objects with rotation, scaling, and transforms
- Apply color tinting and brightness adjustments
- Position objects using cell-based coordinates

## Features

- **Protocol Layer** (`dev.tamboui.ratty.protocol`) - Low-level RGP command generation
- **Object Manager** (`dev.tamboui.ratty.RattyGraphics`) - Lifecycle management for 3D objects
- **Widget** (`dev.tamboui.ratty.widget.Ratty`) - High-level widget for rendering objects
- Support for OBJ and GLB 3D formats
- Chunked payload transmission for large assets
- Transform support: rotation, translation, scaling, depth
- Animation and styling: color tinting, brightness

## Usage

### 1. Protocol Layer (Low-Level)

```java
import dev.tamboui.ratty.protocol.RattyProtocol;
import dev.tamboui.ratty.ObjectFormat;
import dev.tamboui.ratty.PlaceOptions;

// Generate RGP commands
String registerCmd = RattyProtocol.registerByPath(1, ObjectFormat.OBJ, "model.obj");
String placeCmd = RattyProtocol.place(1, PlaceOptions.builder(10, 20, 5, 3)
    .animate(true)
    .rotateY(45)
    .build());
String updateCmd = RattyProtocol.update(1, UpdateOptions.builder()
    .rotateY(90)
    .build());

// Send to terminal output
outputStream.write(registerCmd.getBytes("UTF-8"));
outputStream.write(placeCmd.getBytes("UTF-8"));
```

### 2. Object Manager (Mid-Level)

```java
import dev.tamboui.ratty.RattyGraphics;

try (RattyGraphics graphics = new RattyGraphics(terminal.backend().output())) {
    // Register object
    graphics.registerByPath(1, ObjectFormat.OBJ, "model.obj");
    
    // Place it
    graphics.place(1, PlaceOptions.builder(10, 20, 5, 3)
        .animate(true)
        .rotateY(45)
        .scale(1.5f)
        .build());
    
    // Update rotation
    graphics.update(1, UpdateOptions.builder()
        .rotateY(90)
        .build());
    
    // Objects automatically deleted on close
}
```

### 3. Widget (High-Level)

```java
import dev.tamboui.ratty.widget.Ratty;

// Pre-register object via RattyGraphics
rattyGraphics.registerByPath(1, ObjectFormat.OBJ, "model.obj");

// Create widget
Ratty widget = Ratty.builder()
    .objectId(1)
    .animate(true)
    .rotateY(45)
    .scale(1.5f)
    .depth(2.0f)
    .color("ff8844")
    .brightness(1.2f)
    .build();

// Render (requires raw output)
widget.render(area, buffer, rawOutput);
```

## Protocol Details

### Object Lifecycle

1. **Register**: Upload 3D asset to terminal (by path or payload)
2. **Place**: Position object in terminal cell space
3. **Update**: Modify object's transform/styling (optional)
4. **Delete**: Remove object from terminal

### Commands

| Verb | Description |
|------|-------------|
| `s` | Support query - check if terminal supports RGP |
| `r` | Register object - upload 3D asset (path or payload) |
| `p` | Place object - position in terminal cells |
| `u` | Update object - change transform/styling |
| `d` | Delete object(s) - remove from terminal |

### Transform Options

- **Position**: `row`, `col`, `width`, `height` (in cells)
- **Rotation**: `rx`, `ry`, `rz` (degrees)
- **Translation**: `px`, `py`, `pz` (offset from anchor)
- **Scale**: `scale` (uniform) or `sx`, `sy`, `sz` (non-uniform)
- **Depth**: `depth` (z-offset)
- **Styling**: `color` (RGB hex), `brightness` (multiplier)
- **Animation**: `animate` (boolean)

## Terminal Support

RGP is currently supported by:
- **Ratty** terminal emulator

The protocol uses APC (Application Program Command) escape sequences compatible with xterm-based terminals, but 3D rendering requires terminal-specific support.

## Demo

See `demos/ratty-demo` for a complete example showing:
- Protocol command generation
- Widget usage
- Animated rotation

Run with:
```bash
./gradlew :tamboui-ratty:demos:ratty-demo:run
```

## Architecture

```
┌──────────────────────────────────────────────┐
│  Widget Layer (Ratty widget)                 │
│  - High-level rendering                      │
│  - Dirty tracking                            │
│  - Area-based placement                      │
├──────────────────────────────────────────────┤
│  Object Manager (RattyGraphics)              │
│  - Lifecycle management                      │
│  - Object registration & cleanup             │
│  - Chunked payload transmission              │
├──────────────────────────────────────────────┤
│  Protocol Layer (RattyProtocol)              │
│  - APC command encoding                      │
│  - Capability parsing                        │
│  - Stateless command generation              │
└──────────────────────────────────────────────┘
```

## Module Structure

- `tamboui-ratty/src/main/java/dev/tamboui/ratty/`
  - `ObjectFormat.java` - OBJ/GLB format enum
  - `PlaceOptions.java` - Placement configuration
  - `UpdateOptions.java` - Update configuration
  - `RattyGraphics.java` - Object lifecycle manager
  - `protocol/RattyProtocol.java` - Command encoder
  - `protocol/RattyCapabilities.java` - Terminal capabilities
  - `widget/Ratty.java` - Widget implementation

## Dependencies

- `tamboui-core` - Core types (Buffer, Rect, Widget)
- `tamboui-widgets` - Block widget for wrapping

## References

- [Ratty Graphics Protocol Specification](https://github.com/orhun/ratty/blob/main/protocols/graphics.md)
- [Ratty Terminal Emulator](https://github.com/orhun/ratty)
- [APC Escape Sequences](https://en.wikipedia.org/wiki/C0_and_C1_control_codes#C1_controls)
