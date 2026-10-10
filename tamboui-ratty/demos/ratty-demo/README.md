# Ratty Graphics Protocol Demo

This demo showcases the Ratty Graphics Protocol (RGP) support in TamboUI.

## Usage

### Default: Rotating TamboUI Logo

```bash
jbang ratty-demo
# or explicitly:
jbang ratty-demo --logo
```

Shows a rotating 3D "TUI" logo with RGP protocol documentation and API examples.

### With a custom 3D model

```bash
jbang ratty-demo path/to/model.obj
# or
jbang ratty-demo path/to/model.glb
```

This will:
1. Send an RGP support query to the terminal
2. Register the 3D model with the terminal
3. Place it at the center of the screen with `animate=true`
4. Let the terminal handle smooth native animation
5. Show the documentation UI alongside the 3D object
6. Clean up (delete object) when you quit with 'q'

## Requirements

- **Ratty terminal emulator** with RGP support
- For actual 3D rendering: a `.obj` or `.glb` 3D model file

## Controls

- **q** - Quit the demo

## What You'll See

### With the default TamboUI logo:
- A rotating 3D "TUI" text logo
- Documentation about RGP features
- Code examples showing how to use the API
- Live rotation counter

### With a custom model file:
- The same UI as above
- **Plus**: An actual 3D object rendered by the Ratty terminal
- The object will be placed in the terminal at row 12, column 40
- It will span 20 columns × 10 rows
- Animation is enabled (if supported by your model)

## Protocol Commands Sent

The demo sends these RGP commands:

1. **Support Query**: `ESC _ ratty;g;s ESC \`
   - Checks if the terminal supports RGP

2. **Register Object**: `ESC _ ratty;g;r;id=1;fmt=obj;path=<your-file> ESC \`
   - Registers your 3D model with ID 1

3. **Place Object**: `ESC _ ratty;g;p;id=1;row=12;col=40;w=20;h=10;animate=1;ry=0;scale=1.5;depth=1.0 ESC \`
   - Places the object with native animation enabled
   - Terminal handles rotation automatically

4. **Delete Object** (on quit): `ESC _ ratty;g;d;id=1 ESC \`
   - Cleans up the 3D object when exiting

## Example 3D Models

You can find free 3D models at:
- [Poly Haven](https://polyhaven.com/models) - GLB format
- [Free3D](https://free3d.com/) - OBJ format  
- [Sketchfab](https://sketchfab.com/) - various formats

## Troubleshooting

**"No 3D object visible"**
- Make sure you're running in the Ratty terminal emulator with RGP support
- Verify the model file path is correct (for custom models)
- Try the default logo: `jbang ratty-demo --logo`
- Try a different 3D model (some complex models may not render well)

**"UnsupportedOperationException: Raw output not supported"**
- Your terminal backend doesn't support raw byte output
- This is needed for RGP commands
- Try a different terminal or backend

## Code Example

The demo source shows how to:

```java
// Send support query
String supportQuery = RattyProtocol.supportQuery();
backend.writeRaw(supportQuery.getBytes("UTF-8"));

// Register object
String registerCmd = RattyProtocol.registerByPath(1, ObjectFormat.OBJ, "model.obj");
backend.writeRaw(registerCmd.getBytes("UTF-8"));

// Place object with native animation
PlaceOptions options = PlaceOptions.builder(12, 40, 20, 10)
    .animate(true)  // Terminal handles rotation
    .rotateY(0)
    .scale(1.5f)
    .depth(1.0f)
    .build();
String placeCmd = RattyProtocol.place(1, options);
backend.writeRaw(placeCmd.getBytes("UTF-8"));

// Cleanup on exit
String deleteCmd = RattyProtocol.delete(1);
backend.writeRaw(deleteCmd.getBytes("UTF-8"));
```

## Learn More

- [Ratty Graphics Protocol Spec](https://github.com/orhun/ratty/blob/main/protocols/graphics.md)
- [TamboUI Ratty Module README](../../README.md)
- [Ratty Terminal](https://github.com/orhun/ratty)
