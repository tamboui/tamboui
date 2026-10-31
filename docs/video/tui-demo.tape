Source shared_.tape

# Setup
Hide
Type jbang tui-demo
Enter
Sleep 2
Show

# Recording
Sleep 2
Screenshot screenshots/tui-demo.svg
Set TypingSpeed 1s
Right
Right
Type hhll
Enter
Enter
Space
Space

# Kitty keyboard protocol toggle (p cycles LEGACY -> AUTO -> FULL -> LEGACY).
# Each press surfaces a different key-identity readout: the richer the protocol,
# the more fields the panel reports (kind, logical/shifted/base, text,
# super/hyper/meta).
Type p
Sleep 1
Type p
Sleep 1
Type p
Sleep 1
