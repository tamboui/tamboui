#!/bin/bash

set -e

# Modules that can contain demos
MODULES="tamboui-widgets tamboui-toolkit tamboui-tui tamboui-css tamboui-picocli tamboui-tfx"

usage() {
    echo "Usage: $0 [demo-name] [--native]"
    echo ""
    echo "If no demo name is provided, an interactive selector will be shown."
    echo ""
    echo "Available demos:"

    # List demos from root demos directory
    for dir in demos/*/; do
        if [ -d "$dir" ]; then
            demo=$(basename "$dir")
            if [ "$demo" != "demo-selector" ]; then
                echo "  - $demo"
            fi
        fi
    done

    # List demos from module directories
    for module in $MODULES; do
        if [ -d "$module/demos" ]; then
            for dir in "$module/demos"/*/; do
                if [ -d "$dir" ]; then
                    demo=$(basename "$dir")
                    echo "  - $demo ($module)"
                fi
            done
        fi
    done
    exit 1
}

# Find a demo and return its Gradle path and install directory
# Sets GRADLE_PATH and INSTALL_DIR variables
find_demo() {
    local demo_name="$1"

    # Check root demos directory first
    if [ -d "demos/$demo_name" ]; then
        GRADLE_PATH=":demos:$demo_name"
        INSTALL_DIR="demos/$demo_name/build/install/$demo_name"
        NATIVE_DIR="demos/$demo_name/build/native/nativeCompile"
        return 0
    fi

    # Check module directories
    for module in $MODULES; do
        if [ -d "$module/demos/$demo_name" ]; then
            GRADLE_PATH=":$module:demos:$demo_name"
            INSTALL_DIR="$module/demos/$demo_name/build/install/$demo_name"
            NATIVE_DIR="$module/demos/$demo_name/build/native/nativeCompile"
            return 0
        fi
    done

    return 1
}

run_demo() {
    local demo_name="$1"
    local native="$2"
    local use_exec="$3"

    # Find the demo
    if ! find_demo "$demo_name"; then
        echo "Error: Demo '$demo_name' not found"
        echo ""
        usage
    fi

    if [ "$native" = true ]; then
        echo "Building native image for $demo_name..."
        ./gradlew "$GRADLE_PATH:nativeCompile"
        echo ""
        echo "Running $demo_name (native)..."
        if [ "$use_exec" = true ]; then
            exec "$NATIVE_DIR/$demo_name"
        else
            "$NATIVE_DIR/$demo_name" || true
        fi
    else
        echo "Building $demo_name..."
        ./gradlew "$GRADLE_PATH:installDist"
        echo ""
        echo "Running $demo_name..."
        if [ "$use_exec" = true ]; then
            exec "$INSTALL_DIR/bin/$demo_name"
        else
            "$INSTALL_DIR/bin/$demo_name" || true
        fi
    fi
}

DEMO_NAME=""
NATIVE=false

# Parse arguments
while [ $# -gt 0 ]; do
    case "$1" in
        --native)
            NATIVE=true
            shift
            ;;
        --help|-h)
            usage
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

# If demo name provided directly, run it and exit
if [ -n "$DEMO_NAME" ]; then
    run_demo "$DEMO_NAME" "$NATIVE" true
    exit 0
fi

# Interactive mode: loop showing selector until user quits
echo "Building demo selector..."
./gradlew :demos:demo-selector:installDist -q

while true; do
    # Run the selector and capture the selected demo name
    # The selector prints the demo name to stdout and exits with 0 on selection,
    # or exits with 1 if the user quits without selecting
    set +e
    DEMO_NAME=$(demos/demo-selector/build/install/demo-selector/bin/demo-selector)
    EXIT_CODE=$?
    set -e

    if [ $EXIT_CODE -ne 0 ] || [ -z "$DEMO_NAME" ]; then
        # User quit without selecting
        exit 0
    fi

    echo ""
    run_demo "$DEMO_NAME" "$NATIVE" false
    echo ""
    echo "Demo exited. Returning to selector..."
    echo ""
done
