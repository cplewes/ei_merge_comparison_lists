#!/bin/bash

# Script to rebuild the JAR file with newly compiled classes

JAR_NAME="com.agfa.ris.client.lta_3090.0.13.release_20250728.jar"
ORIGINAL_JAR="$JAR_NAME"
NEW_JAR="${JAR_NAME%.jar}_rebuilt.jar"
TEMP_DIR="temp_jar_extract"

echo "Rebuilding JAR: $ORIGINAL_JAR -> $NEW_JAR"

# Create temporary directory for JAR extraction
mkdir -p "$TEMP_DIR"

# Extract original JAR
echo "Extracting original JAR..."
cd "$TEMP_DIR"
jar -xf "../$ORIGINAL_JAR"
cd ..

# Replace compiled class files
echo "Replacing compiled class files..."
if [ -d "build/classes/com" ]; then
    cp -r build/classes/com/* "$TEMP_DIR/com/"
    echo "Replaced classes in: $TEMP_DIR/com/agfa/ris/client/lta/textarea/"
else
    echo "Error: No compiled classes found in build/classes/"
    exit 1
fi

# Create new JAR
echo "Creating new JAR..."
cd "$TEMP_DIR"
jar -cf "../$NEW_JAR" .
cd ..

# Cleanup
rm -rf "$TEMP_DIR"

if [ -f "$NEW_JAR" ]; then
    echo "Successfully created: $NEW_JAR"
    echo "Original size: $(ls -lh $ORIGINAL_JAR | awk '{print $5}')"
    echo "New size: $(ls -lh $NEW_JAR | awk '{print $5}')"
else
    echo "Failed to create new JAR"
    exit 1
fi