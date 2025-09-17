#!/bin/bash

# Multi-JAR Development Environment Setup Script
# This script initializes the development environment for multiple JAR files

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PROJECT_ROOT/config/jars.yml"

echo "🔧 Setting up multi-JAR development environment..."
echo "Project root: $PROJECT_ROOT"

# Function to extract JAR and organize sources
setup_jar() {
    local jar_id="$1"
    local jar_filename="$2"
    local source_dir="$3"
    local classes_dir="$4"
    local description="$5"

    echo ""
    echo "📦 Setting up JAR: $jar_id ($description)"
    echo "   JAR file: $jar_filename"
    echo "   Source dir: $source_dir"
    echo "   Classes dir: $classes_dir"

    # Check if JAR exists
    local jar_path="$PROJECT_ROOT/jars/$jar_filename"
    if [ ! -f "$jar_path" ]; then
        echo "❌ ERROR: JAR file not found: $jar_path"
        echo "   Please place the JAR file in the jars/ directory"
        return 1
    fi

    # Create directories
    mkdir -p "$PROJECT_ROOT/$source_dir"
    mkdir -p "$PROJECT_ROOT/$classes_dir"

    # Extract JAR and organize sources
    local temp_extract="$PROJECT_ROOT/temp/extract_$jar_id"
    mkdir -p "$temp_extract"

    echo "   📤 Extracting JAR..."
    cd "$temp_extract"
    jar -xf "$jar_path"

    # Move Java sources to organized structure
    echo "   🔄 Organizing source files..."
    if [ -d "com" ]; then
        # Copy Java source files if they exist in the JAR
        find . -name "*.java" -type f | while read -r java_file; do
            local target_file="$PROJECT_ROOT/$source_dir/$java_file"
            mkdir -p "$(dirname "$target_file")"
            cp "$java_file" "$target_file"
        done

        # If no Java files found, we need to decompile
        if [ ! "$(find "$PROJECT_ROOT/$source_dir" -name "*.java" 2>/dev/null)" ]; then
            echo "   ⚙️  No Java sources found, decompiling classes..."

            # Use jadx to decompile if available, otherwise use built-in tools
            if command -v jadx >/dev/null 2>&1; then
                jadx --output-dir "$PROJECT_ROOT/$source_dir" --no-imports --escape-unicode "$jar_path" >/dev/null 2>&1 || true
            else
                echo "   ⚠️  jadx not available, using basic extraction"
                # Copy class files for basic compilation
                find . -name "*.class" -type f | while read -r class_file; do
                    local target_file="$PROJECT_ROOT/$source_dir/$class_file"
                    mkdir -p "$(dirname "$target_file")"
                    cp "$class_file" "$target_file"
                done
            fi
        fi
    fi

    # Clean up temp extraction
    cd "$PROJECT_ROOT"
    rm -rf "$temp_extract"

    echo "   ✅ JAR setup complete: $jar_id"
}

# Function to generate combined classpath
generate_classpath() {
    echo ""
    echo "🔗 Generating combined classpath..."

    local classpath_file="$PROJECT_ROOT/classpath.txt"
    local classpath=""

    # Add all JAR files from libs directory
    if [ -d "$PROJECT_ROOT/libs" ]; then
        for lib_jar in "$PROJECT_ROOT/libs"/*.jar; do
            if [ -f "$lib_jar" ]; then
                classpath="$classpath:$lib_jar"
            fi
        done
    fi

    # Add all original JAR files
    for jar_file in "$PROJECT_ROOT/jars"/*.jar; do
        if [ -f "$jar_file" ]; then
            classpath="$classpath:$jar_file"
        fi
    done

    # Remove leading colon
    classpath="${classpath#:}"

    echo "$classpath" > "$classpath_file"
    echo "   ✅ Classpath written to: $classpath_file"
    echo "   📊 JAR count: $(echo "$classpath" | tr ':' '\n' | grep -c '\.jar$')"
}

# Function to create build scripts
create_build_scripts() {
    echo ""
    echo "🛠️  Creating build scripts..."

    # Create compile-jar.sh
    cat > "$PROJECT_ROOT/scripts/compile-jar.sh" << 'EOF'
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
        SOURCE_DIR="$PROJECT_ROOT/build/lta/src"
        CLASSES_DIR="$PROJECT_ROOT/build/classes/lta"
        ;;
    added)
        SOURCE_DIR="$PROJECT_ROOT/build/added/src"
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
EOF

    chmod +x "$PROJECT_ROOT/scripts/compile-jar.sh"

    # Create rebuild-jar.sh
    cat > "$PROJECT_ROOT/scripts/rebuild-jar.sh" << 'EOF'
#!/bin/bash
# Rebuild specific JAR with OSGi metadata preservation

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
        ORIGINAL_JAR="$PROJECT_ROOT/jars/com.agfa.ris.client.lta_3090.0.13.release_20250728.jar"
        CLASSES_DIR="$PROJECT_ROOT/build/classes/lta"
        OUTPUT_JAR="$PROJECT_ROOT/output/com.agfa.ris.client.lta_3090.0.13.release_20250728_rebuilt.jar"
        PACKAGE_PATH="com/agfa/ris/client/lta/textarea"
        ;;
    added)
        ORIGINAL_JAR="$PROJECT_ROOT/jars/com.agfa.ris.client.studylist.lta.added_3090.0.13.release_20250728.jar"
        CLASSES_DIR="$PROJECT_ROOT/build/classes/added"
        OUTPUT_JAR="$PROJECT_ROOT/output/com.agfa.ris.client.studylist.lta.added_3090.0.13.release_20250728_rebuilt.jar"
        PACKAGE_PATH="com/agfa/ris/client/studylist/lta/added"
        ;;
    *)
        echo "Unknown JAR ID: $JAR_ID"
        exit 1
        ;;
esac

echo "📦 Rebuilding JAR: $JAR_ID"
echo "   Original: $(basename "$ORIGINAL_JAR")"
echo "   Classes: $CLASSES_DIR"
echo "   Output: $(basename "$OUTPUT_JAR")"

if [ ! -f "$ORIGINAL_JAR" ]; then
    echo "❌ Original JAR not found: $ORIGINAL_JAR"
    exit 1
fi

if [ ! -d "$CLASSES_DIR" ]; then
    echo "❌ Classes directory not found: $CLASSES_DIR"
    echo "   Run: ./scripts/compile-jar.sh $JAR_ID"
    exit 1
fi

# Create output directory
mkdir -p "$(dirname "$OUTPUT_JAR")"

# Remove existing rebuilt JAR
if [ -f "$OUTPUT_JAR" ]; then
    echo "   🗑️  Removing existing rebuilt JAR..."
    rm "$OUTPUT_JAR"
fi

# Create temporary extraction directory
TEMP_DIR="$PROJECT_ROOT/temp/rebuild_$JAR_ID"
mkdir -p "$TEMP_DIR"

echo "   📤 Extracting original JAR with metadata..."
cd "$TEMP_DIR"
jar -xf "$ORIGINAL_JAR"

echo "   🔄 Replacing compiled classes..."
if [ -d "$CLASSES_DIR/$PACKAGE_PATH" ]; then
    # Remove old classes and copy new ones
    rm -rf "$PACKAGE_PATH" 2>/dev/null || true
    mkdir -p "$(dirname "$PACKAGE_PATH")"
    cp -r "$CLASSES_DIR/$PACKAGE_PATH" "$(dirname "$PACKAGE_PATH")/"
    echo "   ✅ Replaced classes in: $PACKAGE_PATH"
else
    echo "   ⚠️  No compiled classes found for package: $PACKAGE_PATH"
fi

echo "   📦 Creating new JAR with preserved OSGi metadata..."
jar -cfm "$(basename "$OUTPUT_JAR")" META-INF/MANIFEST.MF .

# Move to output directory
mv "$(basename "$OUTPUT_JAR")" "$OUTPUT_JAR"

# Cleanup
cd "$PROJECT_ROOT"
rm -rf "$TEMP_DIR"

if [ -f "$OUTPUT_JAR" ]; then
    echo "   ✅ Successfully created: $(basename "$OUTPUT_JAR")"
    echo "   📊 Original size: $(ls -lh "$ORIGINAL_JAR" | awk '{print $5}')"
    echo "   📊 New size: $(ls -lh "$OUTPUT_JAR" | awk '{print $5}')"

    # Verify OSGi metadata
    echo ""
    echo "   🔍 Verifying OSGi bundle metadata..."
    jar -tf "$OUTPUT_JAR" | grep -q MANIFEST.MF && echo "   ✅ MANIFEST.MF present" || echo "   ❌ MANIFEST.MF missing"

    # Extract and check key OSGi headers
    jar -xf "$OUTPUT_JAR" META-INF/MANIFEST.MF 2>/dev/null
    if grep -q "Bundle-SymbolicName" META-INF/MANIFEST.MF 2>/dev/null; then
        echo "   ✅ Bundle-SymbolicName preserved"
    else
        echo "   ❌ Bundle-SymbolicName missing"
    fi

    if grep -q "Export-Package" META-INF/MANIFEST.MF 2>/dev/null; then
        echo "   ✅ Export-Package declarations preserved"
    else
        echo "   ❌ Export-Package declarations missing"
    fi

    # Cleanup extracted manifest
    rm -rf META-INF 2>/dev/null
else
    echo "   ❌ Failed to create rebuilt JAR"
    exit 1
fi
EOF

    chmod +x "$PROJECT_ROOT/scripts/rebuild-jar.sh"

    echo "   ✅ Build scripts created"
}

# Main setup function
main() {
    echo "🚀 Multi-JAR Development Environment Setup"
    echo "=========================================="

    # Ensure we're in the right directory
    cd "$PROJECT_ROOT"

    # Create necessary directories
    mkdir -p temp output

    # Setup each JAR according to configuration
    echo ""
    echo "📋 Setting up JARs from configuration..."

    # LTA JAR setup
    setup_jar "lta" \
        "com.agfa.ris.client.lta_3090.0.13.release_20250728.jar" \
        "build/lta/src" \
        "build/classes/lta" \
        "Main LTA textarea components"

    # Generate classpath
    generate_classpath

    # Create build scripts
    create_build_scripts

    echo ""
    echo "✅ Multi-JAR setup complete!"
    echo ""
    echo "📚 Next steps:"
    echo "   1. To add the 'added' JAR: Place it in jars/ directory and run setup again"
    echo "   2. To compile a JAR: ./scripts/compile-jar.sh <jar-id>"
    echo "   3. To rebuild a JAR: ./scripts/rebuild-jar.sh <jar-id>"
    echo "   4. See config/jars.yml for configuration details"
    echo ""
    echo "📁 Directory structure:"
    echo "   jars/           - Original JAR files"
    echo "   build/lta/src/  - LTA JAR source files"
    echo "   build/added/src/ - Added JAR source files (when added)"
    echo "   output/         - Rebuilt JAR files"
    echo "   scripts/        - Build automation scripts"
}

# Run main function
main "$@"