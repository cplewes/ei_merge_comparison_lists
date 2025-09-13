#!/bin/bash

# Create a stub JAR with just the ComparisonStudiesDock class that compiles successfully

echo "Creating stub JAR with successfully compiled classes only..."

# Compile only the classes that don't have errors
CLASSPATH=$(cat classpath.txt)

echo "Attempting to compile ComparisonStudiesDock.java only..."
javac -cp "$CLASSPATH" -d build/classes -sourcepath build/src \
    build/src/com/agfa/ris/client/lta/textarea/docks/dockables/ComparisonStudiesDock.java

if [ $? -eq 0 ]; then
    echo "ComparisonStudiesDock compiled successfully!"

    # Create a minimal JAR with just this class
    JAR_NAME="com.agfa.ris.client.lta_3090.0.13.release_20250728.jar"
    STUB_JAR="${JAR_NAME%.jar}_stub.jar"
    TEMP_DIR="temp_stub_jar"

    mkdir -p "$TEMP_DIR"

    # Extract original JAR
    cd "$TEMP_DIR"
    jar -xf "../$JAR_NAME"

    # Replace only the successfully compiled class
    cp -r ../build/classes/com/agfa/ris/client/lta/textarea/docks ../com/agfa/ris/client/lta/textarea/

    # Create stub JAR
    jar -cf "../$STUB_JAR" .
    cd ..

    rm -rf "$TEMP_DIR"

    echo "Created stub JAR: $STUB_JAR"
else
    echo "Even ComparisonStudiesDock failed to compile. Check dependencies."
fi