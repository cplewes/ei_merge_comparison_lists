#!/bin/bash
# Compile specific JAR sources

set -euo pipefail

if [ $# -eq 0 ]; then
    echo "Usage: $0 <jar-id>"
    echo "Available JARs: lta, added"
    exit 1
fi

JAR_ID="$1"
PROJECT_ROOT="$(dirname "$(dirname "$(realpath "$0")")")"

case "$JAR_ID" in
    lta)
        SOURCE_DIR="$PROJECT_ROOT/src/lta/java"
        CLASSES_DIR="$PROJECT_ROOT/build/classes/lta"
        ;;
    added)
        SOURCE_DIR="$PROJECT_ROOT/src/added/java"
        CLASSES_DIR="$PROJECT_ROOT/build/classes/added"
        ;;
    *)
        echo "Unknown JAR ID: $JAR_ID"
        exit 1
        ;;
esac

echo "🔨 Compiling JAR: $JAR_ID"
echo "   Source: $SOURCE_DIR"
echo "   Output: $CLASSES_DIR"

if [ ! -d "$SOURCE_DIR" ]; then
    echo "❌ Source directory not found: $SOURCE_DIR"
    exit 1
fi

# Create classes directory
mkdir -p "$CLASSES_DIR"

# Read classpath
CLASSPATH=""
if [ -f "$PROJECT_ROOT/classpath.txt" ]; then
    CLASSPATH="$(cat "$PROJECT_ROOT/classpath.txt")"
fi

# Add other compiled classes to classpath for cross-JAR dependencies
for other_classes in "$PROJECT_ROOT/build/classes"/*; do
    if [ -d "$other_classes" ] && [ "$other_classes" != "$CLASSES_DIR" ]; then
        CLASSPATH="$CLASSPATH:$other_classes"
    fi
done

# Find Java files and compile
JAVA_FILES=$(find "$SOURCE_DIR" -name "*.java" 2>/dev/null || true)

if [ -n "$JAVA_FILES" ]; then
    echo "   📊 Compiling $(echo "$JAVA_FILES" | wc -l) Java files..."

    # Compile with classpath
    javac -d "$CLASSES_DIR" -cp "$CLASSPATH" -Xlint:deprecation -Xlint:unchecked $JAVA_FILES

    echo "   ✅ Compilation successful"
    echo "   📁 Classes in: $CLASSES_DIR"
    ls -la "$CLASSES_DIR" 2>/dev/null || true
else
    echo "   ⚠️  No Java files found in: $SOURCE_DIR"
fi
