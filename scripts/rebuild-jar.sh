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

echo "   🔄 Selectively replacing only compiled classes..."
if [ -d "$CLASSES_DIR/$PACKAGE_PATH" ]; then
    # Find and replace only the specific .class files we compiled
    find "$CLASSES_DIR/$PACKAGE_PATH" -name "*.class" -type f | while read -r class_file; do
        # Get relative path from the classes directory
        relative_path="${class_file#$CLASSES_DIR/}"
        target_path="$relative_path"

        # Create target directory if needed
        mkdir -p "$(dirname "$target_path")"

        # Copy the compiled class file, replacing the original
        cp "$class_file" "$target_path"
        echo "     ↻ Updated: $relative_path"
    done
    echo "   ✅ Selectively replaced $(find "$CLASSES_DIR/$PACKAGE_PATH" -name "*.class" -type f | wc -l) compiled classes"
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
