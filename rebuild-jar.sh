#!/bin/bash

# Script to rebuild the JAR file with newly compiled classes while preserving OSGi metadata

JAR_NAME="com.agfa.ris.client.lta_3090.0.13.release_20250728.jar"
ORIGINAL_JAR="$JAR_NAME"
NEW_JAR="${JAR_NAME%.jar}_rebuilt.jar"
TEMP_DIR="temp_jar_extract"

echo "Rebuilding JAR with OSGi metadata preservation: $ORIGINAL_JAR -> $NEW_JAR"

# Remove existing rebuilt JAR if it exists
if [ -f "$NEW_JAR" ]; then
    echo "Removing existing rebuilt JAR..."
    rm "$NEW_JAR"
fi

# Create temporary directory for JAR extraction
mkdir -p "$TEMP_DIR"

# Extract original JAR to preserve all metadata and structure
echo "Extracting original JAR with full metadata..."
cd "$TEMP_DIR"
jar -xf "../$ORIGINAL_JAR"
cd ..

# Replace compiled class files
echo "Replacing compiled class files..."
if [ -d "build/classes/com" ]; then
    # Only replace the specific classes we've modified
    cp -r build/classes/com/agfa/ris/client/lta/textarea/docks "$TEMP_DIR/com/agfa/ris/client/lta/textarea/"
    cp -r build/classes/com/agfa/ris/client/lta/textarea/studylist "$TEMP_DIR/com/agfa/ris/client/lta/textarea/"
    echo "Replaced classes in: $TEMP_DIR/com/agfa/ris/client/lta/textarea/"
    echo "Preserved OSGi manifest and all other bundle metadata"
else
    echo "Error: No compiled classes found in build/classes/"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# Create new JAR preserving directory structure and manifest
echo "Creating new JAR with preserved OSGi metadata..."
cd "$TEMP_DIR"

# Use jar command to create JAR with proper manifest preservation
jar -cfm "../$NEW_JAR" META-INF/MANIFEST.MF .

cd ..

# Cleanup
rm -rf "$TEMP_DIR"

if [ -f "$NEW_JAR" ]; then
    echo "Successfully created: $NEW_JAR"
    echo "Original size: $(ls -lh $ORIGINAL_JAR | awk '{print $5}')"
    echo "New size: $(ls -lh $NEW_JAR | awk '{print $5}')"

    # Verify OSGi metadata is preserved
    echo ""
    echo "Verifying OSGi bundle metadata preservation..."
    BUNDLE_NAME=$(jar -tf "$NEW_JAR" | grep MANIFEST.MF)
    if [ -n "$BUNDLE_NAME" ]; then
        # Extract and check key OSGi headers
        jar -xf "$NEW_JAR" META-INF/MANIFEST.MF 2>/dev/null
        if grep -q "Bundle-SymbolicName.*com.agfa.ris.client.lta" META-INF/MANIFEST.MF 2>/dev/null; then
            echo "✅ Bundle-SymbolicName preserved"
        else
            echo "❌ Bundle-SymbolicName missing"
        fi

        if grep -q "Export-Package.*com.agfa.ris.client.lta" META-INF/MANIFEST.MF 2>/dev/null; then
            echo "✅ Export-Package declarations preserved"
        else
            echo "❌ Export-Package declarations missing"
        fi

        # Cleanup extracted manifest
        rm -rf META-INF 2>/dev/null
        echo "OSGi bundle metadata verification completed"
    else
        echo "❌ No manifest found in rebuilt JAR"
    fi
else
    echo "Failed to create new JAR"
    exit 1
fi