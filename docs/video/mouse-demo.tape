Source shared_.tape

# Setup
Hide
Type jbang mouse-demo
Enter
Sleep 3
Show

# Recording
# Driven entirely by real scripted mouse events (the new tape mouse commands).
# The first event hands control over from the demo's attract loop.
Sleep 800ms

# Right / middle presses glow their tiles
RightClick 30 18
Sleep 600ms
MiddleClick 90 18
Sleep 600ms

# Move the pointer, then scroll over the canvas -> SCROLL tile
MouseMove 60 22
ScrollDown 3 60 22
Sleep 400ms
ScrollUp 2 60 22
Sleep 600ms

# A left click leaves a dot and sets the ink colour
Click 20 30
Sleep 600ms

# Drag draws a stroke across the canvas
Drag 15 32 105 14
Sleep 120ms
Screenshot screenshots/mouse-demo.svg
Sleep 800ms

Type "q"
