#!/bin/bash

set -e

usage() {
    echo "Usage: $0 [demo-name] [--native]"
    echo ""
    echo "If no demo name is provided, an interactive selector will be shown."
    echo ""
    echo "Available demos:"
    for dir in demos/*/; do
        demo=$(basename "$dir")
        if [ "$demo" != "demo-selector" ]; then
            echo "  - $demo"
        fi
    done
    exit 1
}

# Run a demo. If use_exec is true, replaces the current process.
run_demo() {
    local demo_name="$1"
    local native="$2"
    local use_exec="$3"

    # Verify demo exists
    if [ ! -d "demos/$demo_name" ]; then
        echo "Error: Demo '$demo_name' not found in demos/"
        echo ""
        usage
    fi

    if [ "$native" = true ]; then
        echo "Building native image for $demo_name..."
        ./gradlew ":demos:$demo_name:nativeCompile"
        echo ""
        echo "Running $demo_name (native)..."
        if [ "$use_exec" = true ]; then
            exec "demos/$demo_name/build/native/nativeCompile/$demo_name"
        else
            "demos/$demo_name/build/native/nativeCompile/$demo_name" || true
        fi
    else
        echo "Building $demo_name..."
        ./gradlew ":demos:$demo_name:installDist"
        echo ""
        echo "Running $demo_name..."
        if [ "$use_exec" = true ]; then
            exec "demos/$demo_name/build/install/$demo_name/bin/$demo_name"
        else
            "demos/$demo_name/build/install/$demo_name/bin/$demo_name" || true
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
