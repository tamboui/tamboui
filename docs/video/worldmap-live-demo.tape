Source shared_.tape

# Setup
Hide
Type jbang worldmap-live-demo
Enter
Sleep 4
Show

# Recording
# Bundled offline outline (no network), zoomed on Denmark
Sleep 3
Screenshot screenshots/worldmap-live-demo.svg

# Switch to raster tiles (m: bundled -> vector -> raster) and zoom in
Type "m"
Type "m"
Sleep 1
Type "+"
Sleep 1
Type "+"
Sleep 8
Screenshot screenshots/worldmap-live-demo-raster.svg

# Cycle the raster base layer to satellite imagery
Type "b"
Type "b"
Type "b"
Sleep 8
Screenshot screenshots/worldmap-live-demo-satellite.svg

Type "q"
