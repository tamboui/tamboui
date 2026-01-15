Source shared_.tape

# Setup
Hide
Type jbang rflex-demo
Enter
Sleep 2
Show

# Recording
# Start on "Start" tab
Sleep 2s

# Cycle through flex modes (Start -> Center -> End -> SpaceAround -> SpaceEvenly -> SpaceBetween)
Right
Sleep 1s
Right
Sleep 1s
Right
Sleep 1s
Right
Sleep 1s
Right
Sleep 1s

# Scroll down through examples
Down@500ms 10
Sleep 1s

# Scroll back up
Up@500ms 5
Sleep 1s

# Jump to top
Type g
Sleep 1s

# Go back to Start mode
Left@500ms 5
Sleep 1s

# Increase spacing
Type +
Sleep 500ms
Type +
Sleep 500ms
Type +
Sleep 1s

# Show spacing effect while cycling modes
Right
Sleep 1s
Right
Sleep 1s

# Decrease spacing
Type -
Sleep 500ms
Type -
Sleep 500ms
Type -
Sleep 1s

# Jump to bottom to show more examples
Type G
Sleep 2s

# Jump back to top
Type g
Sleep 1s

# Final view
Sleep 2s

