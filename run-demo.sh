#!/bin/bash

set -e

usage() {
    echo "Usage: $0 <demo-name> [--native]"
    echo ""
    echo "Available demos:"
    for dir in demos/*/; do
        demo=$(basename "$dir")
        echo "  - $demo"
    done
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi

DEMO_NAME=""
NATIVE=false

# Parse arguments
while [ $# -gt 0 ]; do
    case "$1" in
        --native)
            NATIVE=true
            shift
            ;;
        -*)
            echo "Unknown option: $1"
            usage
            ;;
        *)
            if [ -z "$DEMO_NAME" ]; then
                DEMO_NAME="$1"
            else
                echo "Error: Multiple demo names provided"
                usage
            fi
            shift
            ;;
    esac
done

if [ -z "$DEMO_NAME" ]; then
    echo "Error: Demo name is required"
    usage
fi

# Verify demo exists
if [ ! -d "demos/$DEMO_NAME" ]; then
    echo "Error: Demo '$DEMO_NAME' not found in demos/"
    echo ""
    usage
fi

if [ "$NATIVE" = true ]; then
    echo "Building native image for $DEMO_NAME..."
    ./gradlew ":demos:$DEMO_NAME:nativeCompile"
    echo ""
    echo "Running $DEMO_NAME (native)..."
    exec "demos/$DEMO_NAME/build/native/nativeCompile/$DEMO_NAME"
else
    echo "Building $DEMO_NAME..."
    ./gradlew ":demos:$DEMO_NAME:installDist"
    echo ""
    echo "Running $DEMO_NAME..."
    exec "demos/$DEMO_NAME/build/install/$DEMO_NAME/bin/$DEMO_NAME"
fi